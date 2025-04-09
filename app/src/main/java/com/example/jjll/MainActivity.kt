package com.example.jjll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope // 導入 lifecycleScope
import com.example.jjll.ui.navigation.AppDestinations
import com.example.jjll.ui.navigation.JJLLAppNavigation
import com.example.jjll.ui.theme.JJLLTheme // 替換成你的主題路徑
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map // 導入 map
import kotlinx.coroutines.launch
import javax.inject.Inject // 導入 Inject

@AndroidEntryPoint // 告訴 Hilt 可以向這個 Activity 注入依賴
class MainActivity : ComponentActivity() {

    @Inject // Hilt 將在此處注入 AppModule 中提供的 SupabaseClient 實例
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用 State 來決定初始頁面（初始為加載中）
        var startDestination by mutableStateOf<String?>(null) // 初始為 null
        var isLoading by mutableStateOf(true) // 添加一個顯式的加載狀態

        lifecycleScope.launch {
            supabaseClient.auth.sessionStatus
                .map { it is SessionStatus.Authenticated }
                .distinctUntilChanged()
                .collect { isAuthenticated ->
                    startDestination = if (isAuthenticated) {
                        AppDestinations.MAIN_ROUTE // 使用定義的路由常量
                    } else {
                        AppDestinations.LOGIN_ROUTE // 使用定義的路由常量
                    }
                    isLoading = false // 狀態確定，結束加載
                    println("Auth state checked: isAuthenticated=$isAuthenticated, startDestination=$startDestination")
                }
        }

        setContent {
            JJLLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoading) { // 根據 isLoading 狀態顯示加載指示器
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator() // 更好的加載指示
                        }
                    } else if (startDestination != null) { // 確保 startDestination 已確定
                        // 在這裡設置 NavHost
                        JJLLAppNavigation(startDestination = startDestination!!) // 使用非空斷言，因為已檢查 isLoading
                    } else {
                        // 可以顯示一個錯誤狀態或回退狀態，理論上不應該到達這裡
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("無法確定初始頁面")
                        }
                    }
                }
            }
        }
    }
}