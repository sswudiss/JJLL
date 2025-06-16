package com.example.jjll.viewmodel // 或 ui.chat 包

import com.example.jjll.data.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = true, // 初始進入時為 true
    val error: String? = null,
    val sending: Boolean = false // 用於標記消息是否正在發送中
)