package com.example.jjll.data

// 聊天列表項目的數據結構
data class ChatListItem(
    val partnerProfile: UserProfile, // 對方用戶信息
    val lastMessage: LastMessagePreview? // 最後一條消息預覽 (可選)
)