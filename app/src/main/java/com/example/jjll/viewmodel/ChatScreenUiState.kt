package com.example.jjll.viewmodel // 或者 com.example.jjll.viewmodel.state

import com.example.jjll.data.model.ChatMessage
import com.example.jjll.data.model.Profile

data class ChatScreenUiState(
    val isLoadingMessages: Boolean = false,          // 是否正在加載歷史消息
    val messages: List<ChatMessage> = emptyList(),   // 顯示的消息列表
    val messageToSend: String = "",                  // 輸入框中的文本
    val sendingMessage: Boolean = false,           // 是否正在發送消息
    val error: String? = null,                       // 錯誤信息
    val chatPartnerProfile: Profile? = null,         // 聊天對象的 Profile 信息
//    val currentChatId: String? = null                // 當前聊天會話的 ID (可選，主要由 ViewModel 內部管理)
)