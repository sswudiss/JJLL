package com.example.jjll.data

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Long,
    val sender_id: String,
    val receiver_id: String, // 或 conversation_id
    val content: String,
    // 最好使用 kotlinx.datetime.Instant 或 String，並配置好序列化器
    val created_at: String, // 暫用 String，需要處理時區和格式
    // 關聯的發送者信息 (非數據庫字段，查詢時 JOIN)
    val senderProfile: UserProfile? = null // 用戶界面可能需要顯示
)

// 用於插入新消息的數據結構 (不需要 id, created_at)
@Serializable
data class NewMessage(
    val sender_id: String,
    val receiver_id: String, // 或 conversation_id
    val content: String
)
