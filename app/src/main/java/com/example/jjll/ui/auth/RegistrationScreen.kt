package com.example.jjll.ui.auth


import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@OptIn(ExperimentalMaterial3Api::class) // 需要用於 Scaffold, TextField, Button 等
@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel = hiltViewModel(), // 注入 ViewModel
    onRegistrationSuccess: () -> Unit,       // 註冊成功後的回調
    onNavigateBackToLogin: () -> Unit        // 導航回登錄頁的回調
) {
    // --- UI 狀態管理 ---
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordsMatch by remember(password, confirmPassword) {
        mutableStateOf(password == confirmPassword || confirmPassword.isEmpty()) // 初始或匹配時為 true
    }

    // --- 從 ViewModel 收集狀態 ---
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()

    // --- 處理一次性成功事件 ---
    LaunchedEffect(Unit) { // 使用 Unit 確保只訂閱一次
        authViewModel.registrationSuccessEvent.collect {
            Log.d("RegistrationScreen", "收到註冊成功事件，觸發導航。")
            onRegistrationSuccess() // 調用成功回調
        }
    }

    // --- UI 佈局 ---
    Scaffold { paddingValues -> // 使用 Scaffold 提供基礎佈局（可選）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp), // 左右邊距
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("創建您的賬戶", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // 用戶名輸入框
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.trim() }, // 去除前後空格
                label = { Text("用戶名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = error?.contains("用戶名", ignoreCase = true) == true // 如果錯誤信息包含用戶名相關
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 密碼輸入框
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordsMatch = it == confirmPassword // 實時檢查密碼是否匹配
                },
                label = { Text("密碼") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = error?.contains("密碼", ignoreCase = true) == true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 確認密碼輸入框
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordsMatch = password == it // 實時檢查密碼是否匹配
                },
                label = { Text("確認密碼") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = !passwordsMatch && confirmPassword.isNotEmpty() // 僅當不匹配且非空時標紅
            )
            // 顯示密碼不匹配提示
            if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                Text(
                    "兩次輸入的密碼不一致",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }


            // 顯示來自 ViewModel 的錯誤信息
            error?.let {
                // 避免重複顯示密碼不匹配錯誤
                if (!(it.contains("用戶名") || it.contains("密碼"))) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 註冊按鈕
            Button(
                onClick = {
                    // 執行客戶端驗證
                    if (username.isBlank() || password.isBlank()) {
                        // 可以考慮更新 error 狀態或使用其他方式提示
                        Log.w("RegistrationScreen", "用戶名或密碼為空")
                        // authViewModel.setError("用戶名和密碼不能為空") // 示例：讓 ViewModel 處理錯誤顯示
                        return@Button
                    }
                    if (!passwordsMatch) {
                        Log.w("RegistrationScreen", "密碼不匹配")
                        // authViewModel.setError("兩次輸入的密碼不一致")
                        return@Button
                    }
                    // 調用 ViewModel 進行註冊
                    authViewModel.registerUser(username, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // 加載時禁用按鈕
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary // 加載指示器顏色
                    )
                } else {
                    Text("註冊")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 返回登錄鏈接
            ClickableText(
                text = AnnotatedString("已經有賬戶了？ 登錄"),
                onClick = { offset ->
                    // 判斷點擊位置是否在 "登錄" 文字上 (可選，簡單處理直接觸發)
                    onNavigateBackToLogin()
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary, // 鏈接顏色
                    textDecoration = TextDecoration.Underline // 下劃線
                )
            )
        }
    }
}