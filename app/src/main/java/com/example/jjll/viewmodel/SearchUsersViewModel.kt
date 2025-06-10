package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    private val supabaseClient: SupabaseClient // Used to get the current user's ID
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUsersUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val debouncePeriod: Long = 500L // 500毫秒的防抖延遲

    /**
     * Called from the UI whenever the search input text changes.
     * It handles debouncing to prevent making an API call on every keystroke.
     *
     * @param query The new search query text from the TextField.
     */
    fun onSearchQueryChanged(query: String) {
        // Update the query in the UI state immediately
        _uiState.update { it.copy(searchQuery = query) }

        // Cancel any previously scheduled search job
        searchJob?.cancel()

        // If the query is blank, clear the results and stop.
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

        // Schedule a new search job with a delay
        searchJob = viewModelScope.launch {
            delay(debouncePeriod) // Wait for the user to stop typing
            executeSearch(query)
        }
    }

    /**
     * Executes the actual network request to search for users.
     * This is a private function called by the debounced handler.
     *
     * @param query The search query to execute.
     */
    private fun executeSearch(query: String) {
        viewModelScope.launch {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                _uiState.update { it.copy(error = "無法獲取當前用戶信息，請重新登錄。") }
                return@launch
            }

            // Set loading state and indicate that a search has been performed
            _uiState.update { it.copy(isLoading = true, searchPerformed = true, error = null) }

            val result = friendRepository.searchUsers(query, currentUserId)

            // Use fold to handle success and failure of the Result
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
     * Called from the UI to send a friend request to a selected user.
     *
     * @param receiverId The user ID of the person to receive the request.
     * @param message The accompanying message for the friend request.
     */
    fun sendFriendRequest(receiverId: String, message: String) {
        viewModelScope.launch {
            // Optionally, you can set a specific "sending" state here
            val result = friendRepository.sendFriendRequest(receiverId, message)

            result.fold(
                onSuccess = {
                    // Update status to show a success message (e.g., a Snackbar)
                    _uiState.update { it.copy(requestSentStatus = RequestSentStatus.SUCCESS) }
                    // TODO: Optionally, remove the user from the current search results
                    // to prevent sending another request immediately.
                },
                onFailure = {
                    // Update status to show a failure message
                    _uiState.update { it.copy(requestSentStatus = RequestSentStatus.FAILURE) }
                }
            )

            // After 2 seconds, reset the status so the message disappears.
            delay(2000L)
            _uiState.update { it.copy(requestSentStatus = null) }
        }
    }
}