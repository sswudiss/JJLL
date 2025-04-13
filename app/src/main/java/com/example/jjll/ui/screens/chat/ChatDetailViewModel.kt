package com.example.jjll.ui.screens.chat


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.JJLLAuthException
import com.example.jjll.data.Message
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.RealtimeState
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle, // Hilt 注入
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // 從 SavedStateHandle 獲取導航參數 (userId 和 username)
    // 使用 checkNotNull 確保它們存在，如果為 null 會崩潰 (之前已修復導航傳參問題)
    private val contactUserId: String = checkNotNull(savedStateHandle["userId"]) { "聊天對象 userId 未從導航參數中獲取" }
    private val contactUsername: String = checkNotNull(savedStateHandle["username"]) { "聊天對象 username 未從導航參數中獲取" }

    // --- 狀態 Flow ---
    private val _contactProfile = MutableStateFlow<UserProfile?>(null)
    val contactProfile: StateFlow<UserProfile?> = _contactProfile.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    // --- 內部狀態 ---
    private var realtimeJob: Job? = null
    private var currentUserId: String? = null

    init {
        Log.d("ChatDetailViewModel", "初始化聊天詳情: 對象ID=$contactUserId, 對象用戶名=$contactUsername")

        // 立即使用導航參數設置聯繫人信息（至少用戶名），用於頂部欄快速顯示
        // 假設 UserProfile 構造函數允許這樣創建（如果需要 UUID，請轉換）
        _contactProfile.value = UserProfile(userId = contactUserId, username = contactUsername /*, 其他字段設為默認值或 null */)

        // 異步獲取當前用戶ID並加載數據
        viewModelScope.launch {
            try {
                currentUserId = authRepository.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "用戶未登錄，無法加載聊天。"
                    Log.e("ChatDetailViewModel", "初始化失敗: 無法獲取當前用戶ID")
                    return@launch
                }
                Log.d("ChatDetailViewModel", "當前用戶ID: $currentUserId")

                // 可選：如果需要加載聯繫人的完整 Profile (例如頭像等)
                // loadFullContactProfile() // 確保此函數處理 isLoading 狀態

                // 加載歷史消息
                loadMessages() // 會處理 isLoading

                // 啟動實時消息監聽
                startRealtimeUpdates()

            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "初始化過程中出錯", e)
                _error.value = "初始化聊天失敗: ${e.message}"
            }
        }
    }

    // --- 公共方法 (供 UI 調用) ---

    fun onMessageInputChange(newInput: String) {
        _messageInput.value = newInput
    }

    fun sendMessage() {
        val messageContent = _messageInput.value.trim()
        val recipientId = contactUserId // 聊天對象的ID
        val senderId = currentUserId // 當前用戶的ID

        if (messageContent.isEmpty()) {
            Log.w("ChatDetailViewModel", "嘗試發送空消息，已忽略。")
            return
        }
        if (senderId == null) {
            _error.value = "無法發送消息：用戶未登錄。"
            Log.e("ChatDetailViewModel", "sendMessage 失敗: currentUserId 為空。")
            return
        }

        // 清空輸入框 (UI 交互更流暢)
        _messageInput.value = ""

        viewModelScope.launch {
            // 注意：此處未實現樂觀更新 (Optimistic Update)
            // 如果需要，可以在此處立即將消息添加到 _messages Flow 中，
            // 然後在發送失敗時將其移除。

            try {
                Log.d("ChatDetailViewModel", "準備發送消息從 $senderId 到 $recipientId: $messageContent")
                chatRepository.sendMessage(senderId, recipientId, messageContent)
                Log.i("ChatDetailViewModel", "消息發送成功。")
                // 消息發送成功後，依賴 Realtime 更新列表，或者手動刷新：
                // loadMessages() // 如果不依賴 Realtime 添加剛發送的消息

            } catch (e: JJLLAuthException) {
                Log.e("ChatDetailViewModel", "發送消息時認證錯誤", e)
                _error.value = "認證錯誤: ${e.message}。請重新登錄後再試。"
                // 如果做了樂觀更新，需要在此處撤銷
                _messageInput.value = messageContent // 恢復輸入框內容以便用戶重試
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "發送消息時出錯", e)
                _error.value = "發送消息失敗: ${e.message}"
                // 如果做了樂觀更新，需要在此處撤銷
                _messageInput.value = messageContent // 恢復輸入框內容以便用戶重試
            }
        }
    }

    // 可選：如果 UI 需要直接獲取當前用戶 ID (例如用於判斷消息對齊)
    fun getCurrentUserId(): String? {
        return currentUserId
    }

    // --- 私有輔助方法 ---

    // 可選：加載完整的聯繫人 Profile 信息
    private fun loadFullContactProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = profileRepository.getProfile(contactUserId)
                _contactProfile.value = profile // 更新完整的 Profile
                if (profile == null) {
                    _error.value = "加載聯繫人資料失敗。"
                }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "加載聯繫人資料時出錯", e)
                _error.value = "錯誤: ${e.message}"
            } finally {
                // 考慮到 loadMessages 可能也會設置 isLoading，這裡的控制需要小心
                // _isLoading.value = false
            }
        }
    }

    private fun loadMessages() {
        val userId = currentUserId ?: return // 確保 ID 有效
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("ChatDetailViewModel", "正在加載用戶 $userId 和 $contactUserId 之間的歷史消息")
                val historicalMessages = chatRepository.getMessagesBetweenUsers(userId, contactUserId)
                // 按創建時間排序
                val sortedMessages = historicalMessages.sortedBy { it.createdAt }
                _messages.value = sortedMessages
                Log.d("ChatDetailViewModel", "已加載 ${sortedMessages.size} 條消息。")
            } catch (e: JJLLAuthException) {
                Log.e("ChatDetailViewModel", "加載消息時認證錯誤", e)
                _error.value = "認證錯誤: ${e.message}。請重新登錄。"
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "加載消息時出錯", e)
                _error.value = "加載歷史消息失敗: ${e.message}"
                _messages.value = emptyList() // 出錯時清空
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startRealtimeUpdates() {
        val userId = currentUserId ?: run {
            Log.e("ChatDetailViewModel", "無法啟動實時更新：當前用戶 ID 為空。")
            return
        }
        realtimeJob?.cancel() // 取消之前的監聽（如果有的話）
        Log.d("ChatDetailViewModel", "正在為用戶 $userId 和聯繫人 $contactUserId 啟動 Realtime 消息監聽")

        realtimeJob = viewModelScope.launch {
            try {
                chatRepository.createMessagesFlow(userId, contactUserId)
                    .catch { e -> // 捕獲 Flow 內部的錯誤
                        if (e is CancellationException) throw e // 重新拋出取消異常
                        Log.e("ChatDetailViewModel", "Realtime Flow 出錯", e)
                        _error.value = "實時連接錯誤: ${e.message}"
                        // 可以在此處添加重試邏輯或通知用戶
                    }
                    .collect { action -> // 收集 Realtime 事件
                        handleRealtimeAction(action, userId)
                    }
            } catch (e: CancellationException) {
                Log.i("ChatDetailViewModel", "Realtime 監聽任務被取消。")
                // Flow 被取消是正常的，例如 ViewModel 清理時
            } catch (e: Exception) {
                // 捕獲創建 Flow 時的初始錯誤 (例如網絡問題)
                Log.e("ChatDetailViewModel", "啟動 Realtime Flow 失敗", e)
                _error.value = "無法啟動實時更新: ${e.message}"
            }
        }
        Log.d("ChatDetailViewModel", "Realtime 監聽任務已啟動: ${realtimeJob?.isActive}")

        // (可選但推薦) 監聽 Realtime 連接狀態
        listenRealtimeStatus()
    }

    // 在 ChatDetailViewModel.kt 中
    private fun handleRealtimeAction(action: PostgresAction, currentUserId: String) {
        Log.d("ChatDetailViewModel", "收到 Realtime action: $action")
        try {
            when (action) {
                is PostgresAction.Insert -> {
                    val newMessage = action.decodeRecord<Message>()
                    Log.d("ChatDetailViewModel", "收到新消息: ${newMessage.content}")
                    // *** 客戶端過濾邏輯 ***
                    val isRelevant = (newMessage.senderId == currentUserId && newMessage.receiverId == contactUserId) ||
                            (newMessage.senderId == contactUserId && newMessage.receiverId == currentUserId)

                    if (isRelevant && messages.value.none { it.id == newMessage.id }) {
                        _messages.update { currentList ->
                            (currentList + newMessage).sortedBy { it.createdAt }
                        }
                        Log.i("ChatDetailViewModel", "相關新消息已添加到 UI 列表。")
                    } else if (!isRelevant) {
                        Log.d("ChatDetailViewModel", "忽略與當前對話無關的消息: ${newMessage.id}")
                    } else {
                        Log.w("ChatDetailViewModel", "忽略重複的消息: ${newMessage.id}")
                    }
                }
                // --- 添加 Update 分支 ---
                is PostgresAction.Update -> {
                    val updatedMessage = action.decodeRecord<Message>()
                    Log.d("ChatDetailViewModel", "消息更新: ${updatedMessage.id}")
                    // *** 客戶端過濾邏輯 ***
                    val isRelevant = (updatedMessage.senderId == currentUserId && updatedMessage.receiverId == contactUserId) ||
                            (updatedMessage.senderId == contactUserId && updatedMessage.receiverId == currentUserId)

                    if (isRelevant) {
                        _messages.update { currentList ->
                            currentList.map { if (it.id == updatedMessage.id) updatedMessage else it }
                                .sortedBy { it.createdAt }
                        }
                        Log.i("ChatDetailViewModel", "相關消息已在 UI 列表中更新。")
                    } else {
                        Log.d("ChatDetailViewModel", "忽略與當前對話無關的消息更新: ${updatedMessage.id}")
                    }
                }
                // --- 添加 Delete 分支 ---
                is PostgresAction.Delete -> {
                    val deletedId = action.oldRecord["id"]?.toString() // 假設 ID 字段名為 'id'
                    Log.d("ChatDetailViewModel", "消息刪除: $deletedId")
                    if (deletedId != null) {
                        val initialSize = messages.value.size
                        _messages.update { currentList ->
                            currentList.filterNot { it.id == deletedId }
                        }
                        if (messages.value.size < initialSize) {
                            Log.i("ChatDetailViewModel", "ID 為 $deletedId 的消息已從 UI 列表中移除。")
                        } else {
                            Log.d("ChatDetailViewModel", "刪除的消息 $deletedId 不在當前列表中。")
                        }
                    }
                }
                // --- 添加 Select 分支 ---
                is PostgresAction.Select -> {
                    // Select 事件通常在監聽 INSERT/UPDATE/DELETE 時不處理
                    Log.d("ChatDetailViewModel", "收到 Realtime SELECT action (已忽略): $action")
                }
                // 如果 PostgresAction 還有其他子類型，也需要添加
            }
        } catch (e: Exception) {
            Log.e("ChatDetailViewModel", "處理 Realtime action 時出錯", e)
        }
    }

    // 可選：監聽 Realtime 連接狀態
    private fun listenRealtimeStatus() {
        viewModelScope.launch {
            chatRepository.getRealtimeStatusFlow() // 假設 ChatRepository 提供此 Flow
                .collect { status ->
                    Log.i("ChatDetailViewModel", "Realtime 狀態: $status")
                    when (status) {
                        is RealtimeState.Closed -> _error.value = "實時連接已關閉。"
                        is RealtimeState.Error -> _error.value = "實時連接錯誤: ${status.message}"
                        RealtimeState.Connecting -> Log.d("ChatDetailViewModel", "實時服務連接中...")
                        RealtimeState.Open -> {
                            // 連接成功，清除相關錯誤信息
                            if (_error.value?.startsWith("實時連接") == true || _error.value?.startsWith("無法啟動") == true) {
                                _error.value = null
                            }
                            Log.i("ChatDetailViewModel", "實時連接已打開。")
                        }
                        // 可能還有其他狀態
                    }
                }
        }
    }


    override fun onCleared() {
        Log.d("ChatDetailViewModel", "ViewModel 清理，取消 Realtime 任務。")
        realtimeJob?.cancel() // 取消協程任務
        // 確保離開 Realtime Channel
        viewModelScope.launch {
            try {
                chatRepository.leaveChannel() // 假設 ChatRepository 提供了離開頻道的方法
                Log.d("ChatDetailViewModel", "已嘗試離開 Realtime Channel。")
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "離開 Realtime Channel 時出錯", e)
            }
        }
        super.onCleared()
    }
}