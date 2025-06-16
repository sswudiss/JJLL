package com.example.jjll.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class NewChatMessage(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String
    // is_read 和 created_at 會有數據庫默認值，所以不需要在這裡指定
)