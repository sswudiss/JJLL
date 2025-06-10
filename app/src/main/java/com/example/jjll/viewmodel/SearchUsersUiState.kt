// 在 viewmodel 包下，或一個新的 ui/search 包下
package com.example.jjll.viewmodel

import com.example.jjll.data.model.Profile

/**
 * Represents the state of the SearchUsersScreen.
 */
data class SearchUsersUiState(
    val searchQuery: String = "",
    val searchResults: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchPerformed: Boolean = false, // Distinguishes initial state from an empty search result
    val requestSentStatus: RequestSentStatus? = null // For showing a temporary feedback message
)

/**
 * Represents the outcome of a friend request action.
 */
enum class RequestSentStatus {
    SUCCESS,
    FAILURE
}