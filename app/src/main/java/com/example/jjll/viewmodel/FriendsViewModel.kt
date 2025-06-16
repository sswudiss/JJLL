package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.model.Friendship
import com.example.jjll.data.repository.ChatRepository
import com.example.jjll.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val chatRepository: ChatRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState = _uiState.asStateFlow()

    //為了支持紅點通知，我們需要在 FriendsViewModel 中暴露一個狀態，用於指示是否存在未處理的好友請求
    // 新增：一個只暴露是否有新請求的 StateFlow
    val hasNewRequests: StateFlow<Boolean> = _uiState
        .map { it.friendRequests.isNotEmpty() } // 當請求列表不為空時，發出 true
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 在有觀察者時保持活躍
            initialValue = false
        )


    // 使用 SharedFlow 處理一次性的導航事件
    private val _navigateToChat = MutableSharedFlow<String>() // Emits chatId
    val navigateToChat = _navigateToChat.asSharedFlow()

    private var _isLoadingChat = MutableStateFlow(false)
    val isLoadingChat = _isLoadingChat.asStateFlow()

    init {
        // ViewModel 初始化時自動加載所有數據
        loadFriendsAndRequests()
    }

    /**
     * 同時載入好友請求和好友清單。
     * 可以呼叫初始載入和手動刷新。
     */
    fun loadFriendsAndRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "用戶未登錄") }
                return@launch
            }

            // 使用 async 並行執行兩個網絡請求
            val requestsDeferred =
                async { friendRepository.getReceivedFriendRequests(currentUserId) }
            val friendsDeferred = async { friendRepository.getFriends(currentUserId) }

            // 等待兩個請求都完成
            val requestsResult = requestsDeferred.await()
            val friendsResult = friendsDeferred.await()

            // 處理結果
            requestsResult.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "無法加載好友請求: ${e.message}"
                    )
                }
                return@launch // 如果一個失敗，可以選擇中止
            }

            friendsResult.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "無法加載好友列表: ${e.message}"
                    )
                }
                return@launch
            }

            // 兩個請求都成功
            _uiState.update {
                it.copy(
                    isLoading = false,
                    friendRequests = requestsResult.getOrThrow(),
                    friends = friendsResult.getOrThrow()
                )
            }
        }
    }

    /**
     * 接受好友請求。
     */
    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            friendRepository.acceptFriendRequest(requestId).onSuccess {
                // 接受成功後，刷新整個列表以反映變化
                // (好友請求消失，好友列表增加)
                loadFriendsAndRequests()
            }.onFailure { e ->
                _uiState.update { it.copy(error = "接受請求失敗: ${e.message}") }
            }
        }
    }

    /**
     * Declines a friend request.
     */
    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            friendRepository.declineFriendRequest(requestId).onSuccess {
                // 拒絕成功後，從 UI 狀態中移除該請求
                _uiState.update { currentState ->
                    currentState.copy(
                        friendRequests = currentState.friendRequests.filterNot { it.id == requestId }
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "拒絕請求失敗: ${e.message}") }
            }
        }
    }

    /**
     * 當用戶點擊好友列表中的一個好友時調用。
     * 此方法會查找或創建一個聊天室，然後發出一個包含 chatId 的導航事件。
     */
    fun onFriendClick(friendship: Friendship) {
        viewModelScope.launch {
            // 獲取對方用戶的 ID
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            // friendProfile 應該在 getFriends() 中被填充
            val otherUserId = friendship.friendProfile?.userId

            if (currentUserId == null || otherUserId == null) {
                _uiState.update { it.copy(error = "無法獲取用戶信息，請重新登錄。") }
                return@launch
            }

            _isLoadingChat.value = true // 開始加載聊天，可以在 UI 上顯示一個小的加載指示器

            // 調用 repository 獲取或創建聊天室
            val result = chatRepository.findOrCreateChatWithUser(otherUserId)

            _isLoadingChat.value = false // 結束加載

            result.fold(
                onSuccess = { chatId ->
                    // 成功獲取 chatId，發出導航事件
                    _navigateToChat.emit(chatId)
                },
                onFailure = { throwable ->
                    // 處理錯誤
                    _uiState.update { it.copy(error = "無法開始聊天: ${throwable.message}") }
                }
            )
        }
    }

}