package com.example.jjll.ui.navigation


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.jjll.ui.screens.chat.ChatDetailScreen
import com.example.jjll.ui.screens.chat.ChatListScreen
import com.example.jjll.ui.screens.contacts.ContactsScreen
import com.example.jjll.ui.screens.profile.MyProfileScreen


// 主屏幕 Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavHostController, // NavController for the main content area
    onLogout: () -> Unit // Callback for logout navigation
) {
    // 底部導航欄項目列表
    val bottomNavItems = listOf(
        BottomNavItem.Contacts,
        BottomNavItem.ChatList,
        BottomNavItem.MyProfile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        // 使用 AppDestinations 中的路由字符串進行比較
                        selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true,
                        onClick = {
                            mainNavController.navigate(item.destination.route) {
                                popUpTo(mainNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 主內容區域的導航
        NavHost(
            navController = mainNavController,
            // 使用 AppDestinations 中的路由字符串作為起始目標
            startDestination = AppDestinations.Contacts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 通訊錄頁面
            composable(AppDestinations.Contacts.route) {
                ContactsScreen(navController = mainNavController)
            }
            // 聊天列表頁面
            composable(AppDestinations.ChatList.route) {
                ChatListScreen(navController = mainNavController)
            }
            // 個人資料頁面
            composable(AppDestinations.MyProfile.route) {
                MyProfileScreen(
                    onLogout = onLogout,
                    navController = mainNavController
                )
            }
            // 聊天詳情頁面 (使用 AppDestinations.ChatDetail 中的定義)
            composable(
                // 使用 routeWithArgs 定義路由模式
                route = AppDestinations.ChatDetail.routeWithArgs,
                // 使用 arguments 定義參數列表
                arguments = AppDestinations.ChatDetail.arguments
            ) { backStackEntry ->
                // ViewModel 通過 SavedStateHandle 獲取參數，無需從 backStackEntry 手動提取
                ChatDetailScreen(navController = mainNavController)
            }
            // --- 其他可能的導航目標 ---
        }
    }
}