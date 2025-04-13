package com.example.jjll.data

import kotlinx.serialization.Serializable

// --- ProfileUpdate 數據類 ---
// 字段名與 UserProfile 一致
@Serializable
data class ProfileUpdate(
    val username: String? = null,
    val displayName: String? = null,
    val avatarURL: String? = null
)