package com.example.jjll.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jjll.ui.auth.LoginScreen
import com.example.jjll.ui.auth.RegistrationScreen


//使用 rememberNavController 創建導航控制器，並設置 NavHost 來管理屏幕切換
// --- 創建應用程序導航圖 ---
@Composable
fun JJLLAppNavigation(startDestination: String) {
    val navController = rememberNavController() // 創建 NavController

    NavHost(navController = navController, startDestination = startDestination) {
        // 定義 LoginScreen 的路由
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToRegister = {
                    // 從登錄頁跳轉到註冊頁
                    navController.navigate(AppDestinations.REGISTER_ROUTE)
                },
                onLoginSuccess = {
                    // 登錄成功後跳轉到主界面
                    // 清除登錄頁之上的所有頁面（如果有的話），並確保主界面是唯一頂層
                    navController.navigate(AppDestinations.MAIN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true } // 從棧中移除登錄頁
                        launchSingleTop = true // 如果主界面已在棧頂，避免重複創建
                    }
                }
                // ViewModel 會通過 hiltViewModel() 自動注入
            )
        }

        // 定義 RegistrationScreen 的路由
        composable(AppDestinations.REGISTER_ROUTE) {
            RegistrationScreen(
                onNavigateToLogin = {
                    // 從註冊頁跳轉回登錄頁
                    navController.navigate(AppDestinations.LOGIN_ROUTE){
                        // 可選: 從註冊頁返回登錄頁時，是否也應該是 single top? 通常是的。
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true } // 返回登錄頁，如果棧里已有登錄頁，用它
                        launchSingleTop = true
                        // 或者簡單 pop 回去: navController.popBackStack()
                    }
                    // navController.popBackStack() // 另一種方式，直接返回上一頁 (如果登錄頁是上一頁)
                },
                onRegisterSuccess = {
                    // 註冊成功後跳轉到主界面
                    // 同樣需要清除登錄/註冊流程
                    navController.navigate(AppDestinations.MAIN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true } // 清除包括登錄頁在內的所有頁面
                        // 如果是從註冊來的，可能需要 popUpTo(graph.startRoute) 更通用？
                        // 或者直接 popUpTo 最開始的路由? 取決於你的導航圖結構
                        launchSingleTop = true
                    }
                }
            )
        }

        // 定義 MainScreen 的路由 (需要創建 MainScreen.kt)
        composable(AppDestinations.MAIN_ROUTE) {
            // TODO: 創建 MainScreen Composable
            MainScreen(navController = navController) // MainScreen 可能也需要 navController 來處理登出等操作
        }

        // --- TODO: 添加其他頁面的路由，例如聊天詳情頁 ---
        // composable(
        //    route = AppDestinations.CHAT_DETAIL_ROUTE_WITH_ARGS,
        //    arguments = listOf(navArgument(AppDestinations.CHAT_DETAIL_ID_ARG) { type = NavType.StringType })
        // ) { backStackEntry ->
        //    val conversationId = backStackEntry.arguments?.getString(AppDestinations.CHAT_DETAIL_ID_ARG)
        //    if (conversationId != null) {
        //        ChatDetailScreen(navController = navController, conversationId = conversationId)
        //    } else {
        //        // 處理參數缺失的情況，例如導航回列表頁
        //        navController.popBackStack()
        //    }
        // }
    }
}