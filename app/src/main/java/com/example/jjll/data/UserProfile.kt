package com.example.jjll.data

import kotlinx.serialization.Serializable


//使用者資料
@Serializable // 使其可以被 Supabase Kotlin SDK 序列化/反序列化
data class UserProfile(
    val userId: String, // 與 Supabase Auth user ID 關聯，應該是主鍵或有唯一索引
    val username: String, // 用戶選擇的唯一用戶名
    val displayName: String? = null, // 用於顯示的名稱，可修改
    val avatarURL: String? = null, // 頭像 URL (來自 Supabase Storage)
    val createdAt: String? = null // 由數據庫自動生成 (如果列配置如此) - String 或 Instant 類型，取決於你的配置和反序列化設置
    // 注意：字段名需要與 Supabase `profiles` 表的列名完全匹配（或使用 @SerialName 註解）
)

// 確保在 app/build.gradle.kts 中應用了 Kotlinx Serialization 插件
// plugins {
//     ...
//     kotlin("plugin.serialization") version "1.9.22" // 版本應與 Kotlin 版本一致
// }

// 並添加依賴 (通常 Supabase-kt 已包含)
// dependencies {
//     implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
// }