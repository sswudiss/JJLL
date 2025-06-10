package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // ViewModel 初始化時自動加載所有數據
        loadFriendsAndRequests()
    }

    /**
     * Loads both friend requests and the friends list concurrently.
     * Can be called for initial load and for manual refresh.
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
            val requestsDeferred = async { friendRepository.getReceivedFriendRequests(currentUserId) }
            val friendsDeferred = async { friendRepository.getFriends(currentUserId) }

            // 等待兩個請求都完成
            val requestsResult = requestsDeferred.await()
            val friendsResult = friendsDeferred.await()

            // 處理結果
            requestsResult.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "無法加載好友請求: ${e.message}") }
                return@launch // 如果一個失敗，可以選擇中止
            }

            friendsResult.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "無法加載好友列表: ${e.message}") }
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
     * Accepts a friend request.
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
}