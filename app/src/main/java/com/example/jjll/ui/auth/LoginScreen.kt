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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@OptIn(ExperimentalMaterial3Api::class) // 需要用於 Scaffold, TextField, Button 等
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = hiltViewModel(), // 注入 ViewModel
    onLoginSuccess: () -> Unit,          // 登錄成功後的回調
    onNavigateToRegister: () -> Unit     // 導航到註冊頁的回調
) {
    // --- UI 狀態管理 ---
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current // 獲取焦點管理器

    // --- 從 ViewModel 收集狀態 ---
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()

    // --- 處理一次性成功事件 ---
    LaunchedEffect(Unit) {
        authViewModel.loginSuccessEvent.collect {
            Log.d("LoginScreen", "收到登錄成功事件，觸發導航。")
            onLoginSuccess() // 調用成功回調
        }
    }

    // --- UI 佈局 ---
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp), // 左右邊距
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("歡迎回來！", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // 用戶名輸入框
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.trim()
                    // 用戶開始輸入時清除錯誤
                    if (error != null) authViewModel.clearError()
                },
                label = { Text("用戶名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next // 鍵盤動作：下一個
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) } // 移動焦點到下一個輸入框
                ),
                isError = error != null // 如果有任何錯誤，標記輸入框（可以更精細判斷）
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 密碼輸入框
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    // 用戶開始輸入時清除錯誤
                    if (error != null) authViewModel.clearError()
                },
                label = { Text("密碼") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // 鍵盤動作：完成
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus() // 清除焦點，收起鍵盤
                        // 觸發登錄操作
                        if (username.isNotBlank() && password.isNotBlank() && !isLoading) {
                            authViewModel.login(username, password)
                        }
                    }
                ),
                isError = error != null // 如果有任何錯誤，標記輸入框
            )

            // 顯示錯誤信息
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 登錄按鈕
            Button(
                onClick = {
                    focusManager.clearFocus() // 收起鍵盤
                    // 調用 ViewModel 進行登錄
                    authViewModel.login(username, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank() // 加載中或輸入為空時禁用
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("登錄")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 註冊鏈接
            ClickableText(
                text = AnnotatedString("還沒有賬戶？ 立即註冊"),
                onClick = { offset ->
                    // 判斷點擊位置（可選）
                    onNavigateToRegister() // 觸發導航到註冊頁的回調
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
    }
}