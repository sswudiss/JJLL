package com.example.jjll.viewmodel // 或 ui.chat 包

import com.example.jjll.data.model.Chat

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val error: String? = null
)