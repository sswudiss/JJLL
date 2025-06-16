package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.repository.FriendRepository
import com.example.jjll.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchUsersViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val profileRepository: ProfileRepository, // 假設需要它來檢查
    private val supabaseClient: SupabaseClient // 用於取得目前使用者的ID
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUsersUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val debouncePeriod: Long = 500L // 500毫秒的防抖延遲

    /**
     *每當搜尋輸入文字發生變化時從 UI 呼叫。
     *它處理去抖動以防止每次擊鍵時都進行 API 呼叫。
     *
     * @param query 來自 TextField 的新搜尋查詢文字。
     */
    fun onSearchQueryChanged(query: String) {
        //立即更新 UI 狀態下的查詢
        _uiState.update { it.copy(searchQuery = query) }

        // 取消任何先前安排的搜尋工作
        searchJob?.cancel()

        // 如果查詢為空，則清除結果並停止。
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchPerformed = false,
                    isLoading = false,
                    error = null
                )
            }
            return
        }

        // 延遲安排新的搜尋作業
        searchJob = viewModelScope.launch {
            delay(debouncePeriod) //等待用戶停止輸入
            executeSearch(query)
        }
    }

    /**
     * 執行實際的網路請求來搜尋使用者。
     * 這是由去抖動處理程序呼叫的私有函數。
     * @param query 要執行的搜尋查詢。
     */
    private fun executeSearch(query: String) {
        viewModelScope.launch {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                _uiState.update { it.copy(error = "無法獲取當前用戶信息，請重新登錄。") }
                return@launch
            }

            // 設定載入狀態並指示已執行搜尋
            _uiState.update { it.copy(isLoading = true, searchPerformed = true, error = null) }

            val result = friendRepository.searchUsers(query, currentUserId)

            // 使用折疊來處理結果的成功和失敗
            result.fold(
                onSuccess = { profiles ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            searchResults = profiles
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "搜索失敗: ${throwable.localizedMessage ?: "未知錯誤"}"
                        )
                    }
                }
            )
        }
    }

    /**
     * 從 UI 呼叫以向選定用戶發送好友請求。
     *
     * @param receiverId 接收請求人的使用者 ID。
     * @param message 好友請求的附帶訊息。
     */
    fun sendFriendRequest(receiverId: String, message: String) {
        viewModelScope.launch {




            // 您也可以在這裡設定特定的「傳送」狀態
            val result = friendRepository.sendFriendRequest(receiverId, message)

            result.fold(
                onSuccess = {
                    //更新狀態以顯示成功訊息（例如，Snackbar）
                    _uiState.update { it.copy(requestSentStatus = RequestSentStatus.SUCCESS) }
                    // TODO: 或者，從目前搜尋結果中刪除用戶，以防止立即發送另一個請求。
                },
                onFailure = { throwable ->
                    // 在這裡處理特定的異常！
                    if (throwable is PostgrestRestException && throwable.message?.contains("duplicate key value violates unique constraint") == true) {
                        _uiState.update { it.copy(requestSentStatus = RequestSentStatus.ALREADY_SENT) }
                    } else {
                        _uiState.update { it.copy(requestSentStatus = RequestSentStatus.FAILURE) }
                    }
                }
            )

            // 2 秒後，重置狀態，使訊息消失。
            delay(2000L)
            _uiState.update { it.copy(requestSentStatus = null) }
        }
    }
}