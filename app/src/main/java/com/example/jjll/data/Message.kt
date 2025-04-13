package com.example.jjll.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


/**
 * 代表一條聊天消息的數據類。
 * 用於應用程序內部以及與 Supabase 的 messages 表進行交互。
 */
@Serializable // 使其可被 Kotlinx Serialization 序列化/反序列化
data class Message(

    /**
     * 消息的唯一標識符。
     * 假設在 Supabase `messages` 表中，主鍵列名為 "id"，類型為 UUID 或 BigInt。
     * 在 Kotlin 中，我們通常將 UUID 作為 String 處理。
     * 從數據庫讀取時應始終存在。
     */
    @SerialName("id") // 映射到數據庫的 "id" 列
    val id: String, // 或 Long，取決於您數據庫的實際主鍵類型

    /**
     * 發送消息的用戶 ID。
     * 關聯到 `profiles` 表的 "id" 列。
     * 假設在 `messages` 表中，列名為 "sender_id"。
     */
    @SerialName("sender_id") // 映射到數據庫的 "sender_id" 列
    val senderId: String, // UUID String

    /**
     * 接收消息的用戶 ID。
     * 關聯到 `profiles` 表的 "id" 列。
     * 假設在 `messages` 表中，列名為 "receiver_id"。
     */
    @SerialName("receiver_id") // 映射到數據庫的 "receiver_id" 列
    val receiverId: String, // UUID String

    /**
     * 消息的文本內容。
     * 假設在 `messages` 表中，列名為 "content"。
     */
    @SerialName("content") // 映射到數據庫的 "content" 列
    val content: String,

    /**
     * 消息創建的時間戳。
     * 假設在 `messages` 表中，列名為 "created_at"，類型為 timestamp with time zone。
     * Supabase 通常以 ISO 8601 格式的字符串返回。
     * 標記為可空，因為它通常由數據庫自動生成。
     */
    @SerialName("created_at") // 映射到數據庫的 "created_at" 列
    val createdAt: String? = null

    // 您可以根據需要添加其他字段，例如：
    // @SerialName("is_read") val isRead: Boolean = false,
    // @SerialName("updated_at") val updatedAt: String? = null
)
// 用於插入新消息的數據結構 (不需要 id, created_at)
@Serializable
data class NewMessage(
    val senderId: String,
    val receiverId: String, // 或 conversation_id
    val content: String
)
