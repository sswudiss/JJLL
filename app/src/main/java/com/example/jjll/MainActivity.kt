package com.example.jjll

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.jjll.ui.auth.AuthViewModel
import com.example.jjll.ui.navigation.AppDestinations
import com.example.jjll.ui.navigation.JJLLAppNavigation
import com.example.jjll.ui.theme.JJLLTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint // 標記 Activity 以啟用 Hilt 注入
class MainActivity : ComponentActivity() {

    // 使用 Hilt 注入 AuthViewModel
    private val authViewModel: AuthViewModel by viewModels()  //import androidx.activity.viewModels

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 決定初始導航目標 ---
        // 在設置 Compose 內容之前，檢查用戶的初始登錄狀態
        // 這裡假設 AuthViewModel 提供了一個簡單的方法來同步檢查初始狀態
        // (更複雜的場景可能需要觀察 Flow 或使用 Splash Screen)
        val startDestination = determineStartDestination()

        setContent {
            JJLLTheme { // 應用主題
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 啟動主導航圖，傳入確定的起始路由
                    JJLLAppNavigation(startDestination = startDestination)
                }
            }
        }
    }

    /**
     * 輔助函數：根據初始認證狀態決定起始路由。
     */
    private fun determineStartDestination(): String {
        // 調用 ViewModel (或直接 Repository) 的方法檢查用戶是否已登錄
        // 假設 isUserLoggedInInitially() 是一個簡單的同步檢查
        return if (authViewModel.isUserLoggedInInitially()) {
            Log.d("MainActivity", "用戶已登錄，起始頁面設置為 main")
            "main" // 主屏幕流程的路由名
        } else {
            Log.d("MainActivity", "用戶未登錄，起始頁面設置為 Login")
            AppDestinations.Login.route // 登錄頁面的路由名
        }
    }
}