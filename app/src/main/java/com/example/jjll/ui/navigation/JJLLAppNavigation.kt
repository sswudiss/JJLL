package com.example.jjll.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jjll.ui.auth.LoginScreen
import com.example.jjll.ui.auth.RegistrationScreen

//設置了包含身份驗證流程和主屏幕（包含底部導航）的導航圖
//JJLLAppNavigation.kt 的核心職責是定義路由和處理頁面之間的跳轉，它通常不直接調用 AuthRepository 的登錄/註冊方法。
// 這些調用是由 LoginScreen 和 RegistrationScreen 內部的 ViewModel 來完成的。


/**
 * 設置應用程序的主要導航圖。
 * 包含身份驗證流程和驗證成功後的主屏幕。
 */
@Composable
fun JJLLAppNavigation(
    // 可以從外部傳入 NavController
    // navController: NavHostController = rememberNavController(),

    // 或者直接在這裡創建 NavController
    navController: NavHostController = rememberNavController(),

    // 需要知道初始狀態是顯示登錄還是主屏幕
    startDestination: String // 例如 AppDestinations.Login.route 或 "main"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 登錄頁面路由
        composable(route = AppDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // 登錄成功後，導航到主屏幕流程 ("main")
                    navController.navigate("main") {
                        popUpTo(AppDestinations.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppDestinations.Registration.route)
                }
            )
        }

        // 註冊頁面路由
        composable(route = AppDestinations.Registration.route) {
            RegistrationScreen(
                onRegistrationSuccess = {
                    // 註冊成功後，導航回登錄頁面
                    navController.navigate(AppDestinations.Login.route) {
                        popUpTo(AppDestinations.Registration.route) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // 主屏幕流程路由 ("main")
        composable(route = "main") {
            // 創建用於 MainScreen 內部導航的 NavController
            val mainNavController = rememberNavController()
            MainScreen(
                mainNavController = mainNavController, // 傳遞內部 NavController
                onLogout = {
                    // 登出時，導航回登錄頁面
                    navController.navigate(AppDestinations.Login.route) {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // --- 其他可能的頂層導航目標 ---
    }
}