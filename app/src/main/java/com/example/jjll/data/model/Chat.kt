package com.example.jjll.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    @SerialName("id") val id: String,
    @SerialName("created_at") val createdAt: String,
    // 手動填充
    var otherParticipantProfile: Profile? = null,
    var lastMessage: ChatMessage? = null
)