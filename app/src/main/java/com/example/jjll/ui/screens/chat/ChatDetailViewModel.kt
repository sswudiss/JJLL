package com.example.jjll.ui.screens.chat

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.common.RepoResult
import com.example.jjll.data.Message
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import com.example.jjll.ui.navigation.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChatDetailViewModel"

// 聊天詳情頁 UI 狀態
data class ChatDetailUiState(
    val contactProfile: UserProfile? = null, // 聊天對象的 Profile
    val messages: List<Message> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val isLoadingContact: Boolean = false,
    val error: String? = null,
    val messageInput: String = ""
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle // Hilt 會自動注入這個，用於獲取導航參數
) : ViewModel() {

    private val _uiState = mutableStateOf(ChatDetailUiState(isLoadingHistory = true, isLoadingContact = true))
    val uiState: androidx.compose.runtime.State<ChatDetailUiState> = _uiState

    private val conversationId: String = savedStateHandle[AppDestinations.CHAT_DETAIL_ID_ARG] ?: "" // 從導航參數獲取對方 ID
    val currentUserId: String? = authRepository.getCurrentUserId() // 獲取當前用戶 ID

    private var messageListenerJob: Job? = null

    init {
        if (conversationId.isNotBlank()) {
            Log.d(TAG, "ViewModel initialized for conversation with: $conversationId")
            fetchContactProfile()
            loadMessageHistory()
            startListeningForNewMessages()
        } else {
            Log.e(TAG, "ViewModel initialized with blank conversationId!")
            _uiState.value = _uiState.value.copy(isLoadingContact = false, isLoadingHistory = false, error = "無效的對話ID")
        }
    }

    // 組件銷毀時取消監聽
    override fun onCleared() {
        super.onCleared()
        stopListeningForNewMessages()
        Log.d(TAG, "ViewModel cleared for conversationId: $conversationId")
    }

    // 獲取聊天對象 Profile (用於頂部欄顯示)
    private fun fetchContactProfile() {
        _uiState.value = _uiState.value.copy(isLoadingContact = true)
        viewModelScope.launch {
            when (val result = profileRepository.getUserProfile(conversationId)) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoadingContact = false, contactProfile = result.data)
                    Log.i(TAG,"Contact profile loaded: ${result.data.username}")
                }
                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingContact = false, error = "無法加載對方信息: ${result.message}")
                    Log.e(TAG,"Error loading contact profile: ${result.message}", result.exception)
                }
                else -> _uiState.value = _uiState.value.copy(isLoadingContact = false)
            }
        }
    }

    // 加載歷史消息
    private fun loadMessageHistory() {
        _uiState.value = _uiState.value.copy(isLoadingHistory = true)
        viewModelScope.launch {
            when (val result = chatRepository.getMessagesWithUser(conversationId)) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoadingHistory = false, messages = result.data)
                    Log.i(TAG,"Message history loaded: ${result.data.size} messages")
                }
                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingHistory = false, error = "無法加載消息記錄: ${result.message}")
                    Log.e(TAG,"Error loading message history: ${result.message}", result.exception)
                }
                else -> _uiState.value = _uiState.value.copy(isLoadingHistory = false)
            }
        }
    }

    // 開始監聽新消息
    private fun startListeningForNewMessages() {
        messageListenerJob?.cancel() // 取消之前的監聽（如果有的話）
        messageListenerJob = viewModelScope.launch {
            // 先確保 Channel 連接
            try {
                chatRepository.connectRealtimeChannel(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to realtime channel initially", e)
                _uiState.value = _uiState.value.copy(error = "無法連接實時服務")
                // 可以考慮添加重試邏輯
            }

            Log.d(TAG, "Starting to collect new messages flow...")
            chatRepository.getNewMessagesFlow(conversationId)
                .catch { e ->
                    Log.e(TAG, "Error in new messages flow", e)
                    _uiState.value = _uiState.value.copy(error = "接收消息時出錯")
                }
                .collectLatest { newMessage -> // 使用 collectLatest 處理背壓
                    Log.i(TAG, "New message received: ${newMessage.content}")
                    // 將新消息添加到現有列表末尾
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + newMessage // 添加新消息
                    )
                }
        }
    }

    // 停止監聽新消息
    private fun stopListeningForNewMessages() {
        messageListenerJob?.cancel()
        messageListenerJob = null
        // 異步斷開連接
        viewModelScope.launch {
            try {
                chatRepository.disconnectRealtimeChannel(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting realtime channel", e)
            }
        }
        Log.d(TAG, "Stopped listening for new messages.")
    }


    // 更新輸入框文本
    fun onMessageInputChange(newText: String) {
        _uiState.value = _uiState.value.copy(messageInput = newText)
    }

    // 發送消息
    fun sendMessage() {
        val contentToSend = _uiState.value.messageInput.trim()
        if (contentToSend.isBlank()) return // 不發送空消息

        Log.d(TAG, "Attempting to send message: $contentToSend")
        // 清空輸入框 (立即反饋)
        _uiState.value = _uiState.value.copy(messageInput = "")

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(conversationId, contentToSend)) {
                is RepoResult.Success -> {
                    Log.i(TAG,"Message sent successfully.")
                    // 消息會通過 Realtime 推送回來，所以這裡不需要手動添加到列表
                    // 如果 Realtime 有延遲，可以考慮在這裡 "樂觀地" 添加一個臨時消息
                }
                is RepoResult.Error -> {
                    Log.e(TAG, "Failed to send message: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "發送失敗: ${result.message}",
                        messageInput = contentToSend // 發送失敗，恢復輸入框內容
                    )
                }
                else -> {}
            }
        }
    }
}