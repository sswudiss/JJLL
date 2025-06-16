package com.example.jjll.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.jjll.ui.chat.ChatListScreen
import com.example.jjll.ui.friends.FriendsScreen
import com.example.jjll.ui.navigation.Screen
import com.example.jjll.viewmodel.AuthViewModel
import com.example.jjll.viewmodel.FriendsViewModel

// 將 Tab 定義移到文件頂部或其自己的文件中，以便重用
private enum class HomeTab(val title: String, val icon: ImageVector) {
    Chats("聊天", Icons.Filled.Chat),
    Contacts("聯繫人", Icons.Filled.Contacts),
    Settings("設置", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    // 我們需要 FriendsViewModel 來獲取紅點狀態，所以在這裡也注入它
    friendsViewModel: FriendsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }
    val friendsUiState by friendsViewModel.uiState.collectAsState() // 可以在這裡觀察整個狀態
    val hasNewRequests by friendsViewModel.hasNewRequests.collectAsState() // 或者只觀察派生出的狀態

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title) },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        // 登出後導航回登錄頁，並清空後退棧
                        navController.navigate(Screen.LOGIN) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }) {
                        Text("登出")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = {
                            if (tab == HomeTab.Contacts) {
                                // 為 "聯繫人" 標籤使用 BadgedBox
                                BadgedBox(badge = {
                                    if (hasNewRequests) {
                                        Badge() // 顯示一個小紅點
                                    }
                                }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                HomeTab.Chats -> {
                    ChatListScreen(
                        onChatClick = { chat ->
                            navController.navigate(Screen.createChatDetailRoute(chat.id))
                        }
                    )
                }
                HomeTab.Contacts -> {
                    // 修正點：將 NavController 傳遞給 FriendsScreen
                    FriendsScreen(
                        navController = navController,
                        viewModel = friendsViewModel // 可以重用已注入的 ViewModel 實例
                    )
                }
                HomeTab.Settings -> {
                    // Placeholder for Settings screen
                    SettingsScreenPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("設置 (待實現)", style = MaterialTheme.typography.headlineMedium)
    }
}