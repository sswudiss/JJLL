package com.example.jjll.viewmodel // 或 ui.friends 包

import com.example.jjll.data.model.FriendRequest
import com.example.jjll.data.model.Friendship

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friendRequests: List<FriendRequest> = emptyList(),
    val friends: List<Friendship> = emptyList(),
    val error: String? = null
)