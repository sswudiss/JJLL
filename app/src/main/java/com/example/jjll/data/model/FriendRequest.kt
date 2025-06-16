// FriendRequest.kt
package com.example.jjll.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    @SerialName("id") val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") var status: String,
    @SerialName("message") val message: String? = null,
    @SerialName("created_at") val createdAt: String,

    // 關鍵修改：
    // JSON 返回的鍵名將是 "requester_profile"，所以 @SerialName 也要匹配
    @SerialName("requester_profile")
    val requesterProfile: Profile? = null
)