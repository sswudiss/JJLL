package com.example.jjll.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    // 假設 contacts 表有 id, sender_id, receiver_id, status, created_at
    // 字段名需與數據庫完全一致，或使用 @SerialName
    @SerialName("id") val id: Long? = null, // 假設主鍵是 BigInt/Serial
    @SerialName("sender_id") val senderId: String, // UUID String
    @SerialName("receiver_id") val receiverId: String, // UUID String
    @SerialName("status") val status: ContactStatus,
    @SerialName("created_at") val createdAt: String? = null // ISO 8601 String format from Supabase timestampz
    // @SerialName("updated_at") val updatedAt: String? = null
)