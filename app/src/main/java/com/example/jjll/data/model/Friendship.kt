package com.example.jjll.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Friendship(
    @SerialName("id") val id: String,
    @SerialName("user1_id") val user1Id: String,
    @SerialName("user2_id") val user2Id: String,
    @SerialName("created_at") val createdAt: String,
    // 我們將通過 JOIN 查詢手動填充好友的 profile
    var friendProfile: Profile? = null
)