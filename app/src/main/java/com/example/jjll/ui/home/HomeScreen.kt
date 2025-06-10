// HomeScreen.kt
package com.example.jjll.ui.home

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.jjll.ui.chat.ChatListScreen
import com.example.jjll.ui.friends.FriendsScreen
import com.example.jjll.ui.navigation.Screen
import com.example.jjll.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState() // UserInfo?
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) } // 假設有標籤

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JJLL (兔子) - ${selectedTab.title}") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.LOGIN) {
                            popUpTo(Screen.HOME) { inclusive = true }
                        }
                    }) {
                        Text("登出")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
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
                            // 使用我們在 AppNavigation 中定義的輔助函數
                            navController.navigate(Screen.createChatDetailRoute(chat.id))
                        }
                    )
                }

                HomeTab.Contacts -> {
                    FriendsScreen(
                        onFriendClick = { friendship ->
                            // TODO: 這裡將觸發進入聊天的邏輯
                            // 1. 調用 ChatRepository 的 findOrCreateChat
                            // 2. 獲取 chatId
                            // 3. 導航到 ChatScreen
                            val otherUserId = friendship.friendProfile?.userId
                            if (otherUserId != null) {
                                // 可以在這裡啟動一個協程調用 repository，
                                // 或者讓 FriendsViewModel 處理這個邏輯
                                // 獲取到 chatId 後...
                                // navController.navigate(Screen.createChatDetailRoute(chatId))
                            }
                        },
                        onAddFriendClick = {
                            navController.navigate(Screen.SEARCH_USERS)
                        }
                    )
                }

                HomeTab.Settings -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("設置 (待實現)", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

// 定義底部導航標籤
enum class HomeTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Chats("聊天", Icons.Filled.Chat),
    Contacts("聯繫人", Icons.Filled.Contacts),
    Settings("設置", Icons.Filled.Settings)
}