package com.example.jjll.ui.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember // 導入 remember
import androidx.compose.runtime.saveable.rememberSaveable // 處理配置更改後狀態恢復
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.ui.unit.dp // 導入 dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jjll.ui.dialogs.SearchUserDialog
import com.example.jjll.ui.screens.BottomNavItem
import com.example.jjll.ui.screens.chat.ChatListScreen
import com.example.jjll.ui.screens.contacts.ContactsScreen
import com.example.jjll.ui.screens.profile.MyProfileScreen
import com.example.jjll.ui.screens.contacts.ContactsViewModel
import com.example.jjll.ui.screens.profile.ProfileViewModel

// TODO: 後續從 ViewModel 獲取真實用戶信息
data class UserInfo(val userId: String = "temp_user_id", val avatarUrl: String? = null)

val sampleUserInfo = UserInfo() // 臨時佔位數據

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // 暫時忽略 Scaffold 的 padding 警告
@OptIn(ExperimentalMaterial3Api::class) // 使用實驗性的 Material 3 API (如 TopAppBar)
@Composable
fun MainScreen(
    navController: NavHostController, // 從 JJLLAppNavigation 傳入
    profileViewModel: ProfileViewModel = hiltViewModel(),
    contactsViewModel: ContactsViewModel = hiltViewModel() // <-- 注入 ContactsVie
) {

    val profileUiState by profileViewModel.uiState
    val contactsUiState by contactsViewModel.uiState // <-- 觀察 ContactsViewModel 的狀態
    // 記住當前選中的導航項的 route，使用 rememberSaveable 可以在配置更改（如旋轉）後恢復狀態
    var selectedRoute by rememberSaveable { mutableStateOf(BottomNavItem.Chat.route) }

    // 控制搜索對話框顯示的狀態
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }

    // 存儲底部導航欄的所有項
    val bottomNavItems = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Contacts,
        BottomNavItem.MyProfile
    )

    // 監聽 ProfileViewModel 的登出導航事件
    LaunchedEffect(
        key1 = profileViewModel,
        key2 = navController
    ) { /* ... logout navigation logic ... */ }


    // TODO: 後續從 ViewModel 獲取用戶信息
    val currentUserInfo = remember { sampleUserInfo }

    Scaffold(
        // --- 頂部應用欄 ---
        topBar = {
            if (selectedRoute != BottomNavItem.MyProfile.route) {
                TopAppBar(
                    title = { /* Text("JJLL 兔子") */ }, // 頂部標題 (可選)
                    navigationIcon = { /* ... 用戶頭像 ... */ },
                    actions = {
                        // 第一個 Action: 搜索按鈕
                        IconButton(
                            // 添加日誌，確認點擊事件被觸發，狀態嘗試改變
                            onClick = {
                                Log.d(
                                    "MainScreen",
                                    "Search icon clicked. Current showSearchDialog: $showSearchDialog"
                                )
                                showSearchDialog = true
                                Log.d(
                                    "MainScreen",
                                    "Search icon clicked. New showSearchDialog: $showSearchDialog"
                                ) // 確認修改後的值
                            }
                            // 可選: 添加 modifier 等其他參數
                        ) { // content lambda 開始
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "搜索" // 無障礙文本
                            )
                        } // content lambda 結束 & IconButton 結束

                        // 第二個 Action: 菜單按鈕
                        IconButton(
                            onClick = {
                                /* TODO: 處理菜單點擊 */
                                println("Menu button clicked (TODO)") // 臨時日誌
                            }
                        ) { // content lambda 開始
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "菜單"
                            )
                        } // content lambda 結束 & IconButton 結束
                    } // actions lambda 結束
                ) // TopAppBar 結束
            }
        },
        // --- 底部導航欄 ---
        bottomBar = {
            NavigationBar { // Material 3 的底部導航
                bottomNavItems.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedRoute = item.route }, // 點擊時更新選中的路由
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) }
                        // 可選：設置顏色
                        // colors = NavigationBarItemDefaults.colors(...)
                    )
                }
            }
        }
    ) { innerPadding -> // Scaffold 提供內邊距，防止內容被 Top/Bottom Bar 遮擋
        // --- 主內容區域 ---
        Box(
            modifier = Modifier
                .padding(innerPadding) // 應用內邊距
                .fillMaxSize()
        ) {
            // 根據選中的路由顯示不同的頁面內容
            when (selectedRoute) {
                BottomNavItem.Chat.route -> ChatListScreen(
                    onChatClick = { userId, userName ->
                        Log.d("MainScreen", "Chat item clicked: userId=$userId, userName=$userName. Navigating...")
                        // 導航到聊天詳情頁，這次也傳了名字 (雖然路由定義目前只接收 ID)
                        // 可以考慮將 userName 作為可選參數添加到路由，或在 ChatDetailViewModel 中根據 ID 獲取
                        navController.navigate(
                            com.example.jjll.ui.navigation.AppDestinations.createChatDetailRoute(userId)
                        )
                    }
                )
                // 使用實際的 ContactsScreen
                BottomNavItem.Contacts.route -> ContactsScreen(
                    onContactClick = { userId -> // <-- 檢查這個 lambda
                        Log.d("MainScreen", "onContactClick in MainScreen received userId: $userId")
                        Log.d(
                            "MainScreen",
                            "Attempting to navigate to: ${
                                com.example.jjll.ui.navigation.AppDestinations.createChatDetailRoute(
                                    userId
                                )
                            }"
                        )
                        try {
                            navController.navigate(
                                com.example.jjll.ui.navigation.AppDestinations.createChatDetailRoute(
                                    userId
                                )
                            )
                            Log.d("MainScreen", "navController.navigate called successfully.")
                        } catch (e: Exception) {
                            Log.e("MainScreen", "Error during navigation", e) // 捕獲導航異常
                        }
                    }
                )

                BottomNavItem.MyProfile.route -> MyProfileScreen(viewModel = profileViewModel)
            }
        }
    }
    // --- 條件渲染搜索對話框 ---
    // 這個 if 塊應該放在 Scaffold 的外部，或者在 Scaffold 內容 lambda 的頂層
    if (showSearchDialog) {
        // 添加日誌確認渲染邏輯被執行
        Log.d(
            "MainScreen",
            "Conditional rendering: showSearchDialog is true, attempting to render SearchUserDialog."
        )
        SearchUserDialog(
            uiState = contactsUiState, // 傳遞狀態
            onQueryChange = contactsViewModel::onSearchQueryChange, // 傳遞查詢回調
            onAddContactClick = { userId ->
                contactsViewModel.sendContactRequest(userId) // 傳遞添加聯繫人回調
                // 可以在這裡關閉對話框，或保持打開以顯示結果
                // showSearchDialog = false
                Log.d("MainScreen", "Add contact button clicked in dialog for userId: $userId")
            },
            onDismiss = { // 傳遞關閉回調
                Log.d("MainScreen", "Search dialog dismiss requested.")
                showSearchDialog = false // 關閉對話框
                contactsViewModel.clearSearchState() // 清理搜索狀態
            }
        )
    }

}

// --- 頁面佔位符 ---
@Composable
fun ChatListScreenPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("聊天列表頁面 (Chat List)")
    }
}

@Composable
fun ContactsScreenPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("通訊錄頁面 (Contacts)")
    }
}

@Composable
fun MyProfileScreenPlaceholder(navController: NavHostController) {
    // 在"我的"頁面實現登出示例
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("我的頁面 (My Profile)")
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    // TODO: 這裡應該調用 ViewModel 清理 Supabase Session
                    // viewModel.signOut() // 假設有 ViewModel
                    // 導航回登錄頁，並清除 MainScreen 之上的所有回退棧
                    navController.navigate(com.example.jjll.ui.navigation.AppDestinations.LOGIN_ROUTE) {
                        popUpTo(com.example.jjll.ui.navigation.AppDestinations.MAIN_ROUTE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // 紅色按鈕
            ) {
                Text("登出賬號")
            }
        }
    }
}