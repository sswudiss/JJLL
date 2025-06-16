// 在 viewmodel 包下，或一個新的 ui/search 包下
package com.example.jjll.viewmodel

import com.example.jjll.data.model.Profile

/**
 *表示 SearchUsersScreen 的狀態。
 */
data class SearchUsersUiState(
    val searchQuery: String = "",
    val searchResults: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchPerformed: Boolean = false, // 搜尋已執行：區分初始狀態和空搜尋結果
    val requestSentStatus: RequestSentStatus? = null //請求發送狀態：用於顯示臨時回饋訊息
)

/**
 * 表示好友請求操作的結果。
 */
enum class RequestSentStatus {
    SUCCESS,
    FAILURE,
    ALREADY_SENT
}