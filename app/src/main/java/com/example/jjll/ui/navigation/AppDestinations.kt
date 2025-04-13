package com.example.jjll.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * 定義應用程序中的導航路由常量和參數。
 * 使用 sealed class 提供類型安全，並方便在 NavHost 和導航調用中引用。
 */
sealed class AppDestinations(val route: String) {

    // --- 身份驗證流程 ---
    object Login : AppDestinations("login")           // 登錄頁面
    object Registration : AppDestinations("registration") // 註冊頁面

    // --- 主屏幕 (MainScreen 內部的目標，由底部導航管理) ---
    object Contacts : AppDestinations("contacts")       // 通訊錄頁面
    object ChatList : AppDestinations("chat_list")      // 聊天列表頁面 (新增)
    object MyProfile : AppDestinations("my_profile")    // 個人資料頁面

    // --- 其他頁面 ---
    object ChatDetail : AppDestinations("chat_detail") { // 聊天詳情頁面 (基礎路由)
        // 參數名稱常量化
        const val userIdArg = "userId"
        const val usernameArg = "username"
        // 完整的路由模式
        val routeWithArgs = "${route}/{${userIdArg}}/{${usernameArg}}"
        // NavArgument 列表
        val arguments = listOf(
            navArgument(userIdArg) { type = NavType.StringType },
            navArgument(usernameArg) { type = NavType.StringType } // 假設 username 總是非空傳遞
            // 如果可能為空，應設為 nullable = true
            // 並在 buildRoute 中處理 null
        )
        // 構建路由路徑的輔助函數
        fun buildRoute(userId: String, username: String): String {
            // 考慮 URL 編碼 username，如果它可能包含特殊字符
            // import java.net.URLEncoder
            // val encodedUsername = URLEncoder.encode(username, "UTF-8")
            // return "${route}/$userId/$encodedUsername"
            return "${route}/$userId/$username" // 簡單拼接
        }
    }

    // 可以添加更多目標...
    // object Settings : AppDestinations("settings")
    // object EditProfile : AppDestinations("edit_profile")

}