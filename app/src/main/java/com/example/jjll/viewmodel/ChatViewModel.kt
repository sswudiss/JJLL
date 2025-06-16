package com.example.jjll.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.repository.ChatRepository
import com.example.jjll.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val supabaseClient: SupabaseClient, // 用於獲取當前用戶ID
    savedStateHandle: SavedStateHandle // 用於獲取導航參數
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    // 從導航參數中獲取 chatId
    private val chatId: String = checkNotNull(savedStateHandle[Screen.CHAT_ID_ARG])

    private var realtimeJob: Job? = null

    init {
        // ViewModel 初始化時加載歷史消息並開始監聽實時消息
        loadInitialMessages()
        subscribeToNewMessages()
    }

    /**
     * Called when the user types in the message input field.
     */
    fun onInputChange(newInput: String) {
        _uiState.update { it.copy(currentInput = newInput) }
    }

    /**
     * Loads the initial batch of historical messages.
     */
    private fun loadInitialMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = chatRepository.getMessages(chatId)
            result.fold(
                onSuccess = { messages ->
                    _uiState.update { it.copy(isLoading = false, messages = messages) }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = "無法加載消息: ${throwable.message}") }
                }
            )
        }
    }

    /**
     * Sends the current input text as a new message.
     */
    fun sendMessage() {
        // 從 state 中獲取當前輸入並去除首尾空格
        val content = _uiState.value.currentInput.trim()
        // 如果內容為空，則不執行任何操作
        if (content.isBlank()) return

        viewModelScope.launch {
            // 立即更新 UI 狀態：清空輸入框，並將 sending 設為 true
            // 這會讓 UI 的發送按鈕變為加載狀態
            _uiState.update { it.copy(sending = true, currentInput = "") } // 標記為發送中並清空輸入框

            val result = chatRepository.sendMessage(chatId, content)

            // Realtime 會處理將新消息添加到列表，所以我們這裡主要是處理發送失敗的情況
            result.fold(
                onSuccess = {
                    // 發送成功！我們不需要手動將消息添加到列表中，
                    // 因為 Realtime 會監聽到這次插入並自動更新列表。
                    // 我們只需將 sending 狀態設回 false。
                    _uiState.update { it.copy(sending = false) }
                },
                onFailure = { throwable ->
                    // 發送失敗，可以考慮將消息狀態標記為"未發送"並提供重試選項
                    _uiState.update {
                        it.copy(
                            sending = false,
                            error = "發送失敗",  // 可以在 UI 上顯示一個臨時錯誤
                            currentInput = content  // 將發送失敗的內容還原回輸入框，方便用戶重試
                        )
                    }
                }
            )
        }
    }


    /**
     * Subscribes to the Realtime flow for new messages in this chat.
     */
    private fun subscribeToNewMessages() {
        // 如果已有訂閱任務，先取消它，防止重複訂閱
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            Log.d("ChatViewModel", "Subscribing to new messages for chat: $chatId")
            chatRepository.getNewMessagesFlow(chatId)
                .catch { e ->
                    Log.e("ChatViewModel", "Realtime flow error: ${e.message}", e)
                    // 處理 Flow 中可能發生的錯誤（例如，網絡中斷）
                    _uiState.update { it.copy(error = "實時連接錯誤: ${e.message}") }
                }
                .collect { newMessage ->
                    Log.d("ChatViewModel", "New message received via Realtime: ${newMessage.content}")
                    // 收到新消息，將其添加到現有的消息列表中
                    _uiState.update { currentState ->
                        // 避免重複添加（有時發送者會立即收到自己消息的廣播）
                        if (currentState.messages.any { it.id == newMessage.id }) {
                            Log.d("ChatViewModel", "Message already exists, skipping.")
                            currentState
                        } else {
                            Log.d("ChatViewModel", "Adding new message to UI state.")
                            currentState.copy(messages = currentState.messages + newMessage)
                        }
                    }
                }
        }
    }

    /**
     * Returns the ID of the currently logged-in user.
     */
    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ChatViewModel", "ViewModel cleared, cancelling realtime job.")
        // 當 ViewModel 被銷毀時（例如用戶離開屏幕），取消協程任務
        // 這會自動取消 Flow 的收集，並幫助 Supabase Realtime SDK 清理頻道訂閱
        realtimeJob?.cancel()
    }
}