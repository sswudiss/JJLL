package com.example.jjll.ui.navigation // 或其他你選擇的路徑

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * 定義應用程序底部導航欄的項目。
 * 每個項目關聯一個 AppDestinations 路由、一個標籤和一個圖標。
 */
sealed class BottomNavItem(
    val destination: AppDestinations, // 關聯的導航目標
    val label: String,                // 顯示的標籤文本
    val icon: ImageVector             // 顯示的圖標
) {
    // 通訊錄頁面
    object Contacts : BottomNavItem(
        destination = AppDestinations.Contacts,
        label = "通訊錄",
        icon = Icons.Default.Contacts // 或 Icons.Outlined.Contacts
    )

    // 聊天列表頁面
    object ChatList : BottomNavItem(
        destination = AppDestinations.ChatList,
        label = "聊天",
        icon = Icons.Default.Chat // 或 Icons.Outlined.Chat
    )

    // 個人資料頁面
    object MyProfile : BottomNavItem(
        destination = AppDestinations.MyProfile,
        label = "我的",
        icon = Icons.Default.Person // 或 Icons.Outlined.Person
    )
}

// 可以在此文件底部或 MainScreen 中定義底部導航項目列表
val bottomNavItemsList = listOf(
    BottomNavItem.Contacts,
    BottomNavItem.ChatList,
    BottomNavItem.MyProfile
)