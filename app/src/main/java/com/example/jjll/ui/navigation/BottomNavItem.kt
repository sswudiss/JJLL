package com.example.jjll.ui.screens // 或其他你選擇的路徑

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.ui.graphics.vector.ImageVector

// 使用 Sealed Class 定義底部導航項
sealed class BottomNavItem(
    val route: String, // 每個項目的唯一標識/路由名
    val title: String, // 顯示的標題
    val selectedIcon: ImageVector, // 選中狀態的圖標
    val unselectedIcon: ImageVector // 未選中狀態的圖標
) {
    object Chat : BottomNavItem(
        route = "chat_list", // 與後續 ChatListScreen 的路由關聯 (也可以用簡單標識符)
        title = "聊天",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    )
    object Contacts : BottomNavItem(
        route = "contacts",
        title = "通訊錄",
        selectedIcon = Icons.Filled.Contacts,
        unselectedIcon = Icons.Outlined.Contacts
    )
    object MyProfile : BottomNavItem(
        route = "my_profile",
        title = "我的",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
}