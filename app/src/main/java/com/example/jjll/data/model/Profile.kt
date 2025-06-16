// Profile.kt
package com.example.jjll.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("user_id") // <--- 修改這裡
    val userId: String,    // <--- 同時修改屬性名 (或者保持 id 但 @SerialName 必須是 "user_id")

    @SerialName("username")
    val username: String?,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String,  // <--- 這個字段是必需的 (非可空)

    @SerialName("display_name") // 從截圖看，你還有一個 display_name 列
    val displayName: String? = null,

    @SerialName("updated_at") // 從截圖看，你還有一個 updated_at 列
    val updatedAt: String? = null
)