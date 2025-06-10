package com.example.jjll.viewmodel

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
        val content = _uiState.value.currentInput.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, currentInput = "") } // 標記為發送中並清空輸入框

            val result = chatRepository.sendMessage(chatId, content)

            // Realtime 會處理將新消息添加到列表，所以我們這裡主要是處理發送失敗的情況
            result.fold(
                onSuccess = {
                    // 成功發送，Realtime 會接收到這條消息並更新UI
                    _uiState.update { it.copy(sending = false) }
                },
                onFailure = { throwable ->
                    // 發送失敗，可以考慮將消息狀態標記為"未發送"並提供重試選項
                    _uiState.update {
                        it.copy(
                            sending = false,
                            error = "發送失敗",
                            currentInput = content // 將未成功發送的內容還原到輸入框
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
        realtimeJob?.cancel() // 確保之前的訂閱已取消
        realtimeJob = viewModelScope.launch {
            chatRepository.getNewMessagesFlow(chatId)
                .catch { e ->
                    _uiState.update { it.copy(error = "實時連接錯誤: ${e.message}") }
                }
                .collect { newMessage ->
                    _uiState.update { currentState ->
                        // 避免重複添加（如果發送成功後立即收到自己的消息）
                        if (currentState.messages.any { it.id == newMessage.id }) {
                            currentState
                        } else {
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
        // ViewModel 銷毀時取消 Realtime 訂閱
        // 注意：Supabase-kt 的 channel 在 scope 結束時通常會自動清理，但顯式取消是好習慣
        realtimeJob?.cancel()
    }
}