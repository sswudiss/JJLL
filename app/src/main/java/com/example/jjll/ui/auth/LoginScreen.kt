package com.example.jjll.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    // navController: NavController, // 方式一：直接傳遞 NavController
    onNavigateToRegister: () -> Unit, // 方式二：使用回調觸發導航
    onLoginSuccess: () -> Unit,      // 登錄成功後的回調
    viewModel: AuthViewModel = hiltViewModel() // Hilt 自動提供 ViewModel
) {
    // 收集 ViewModel 的狀態
    val loginInput = viewModel.loginInput
    val password = viewModel.password
    val passwordVisible = viewModel.passwordVisible
    val authResultState = viewModel.authResultState

    // 監聽一次性導航事件
    LaunchedEffect(key1 = viewModel) { // key1 = viewModel 保證只在 ViewModel 實例變化時重啟動 effect
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateToMain -> onLoginSuccess()
                // NavigationEvent.NavigateToRegister -> onNavigateToRegister() // 可以在ViewModel觸發，但通常UI層按鈕更直觀
                else -> {} // LoginScreen 只關心登錄成功
            }
        }
    }

    // UI 佈局
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp), // 外層 Box 提供基礎 Padding
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()) // 使 Column 內容可滾動，防止小屏幕溢出
                .padding(horizontal = 16.dp), // 內部 Column 的水平 Padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("歡迎回來！", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            // 郵箱輸入框
            OutlinedTextField(
                value = loginInput,
                onValueChange = viewModel::onLoginInputChange, // <--- 調用新的處理函數
                label = { Text("用戶名 (Username)") }, // <--- 修改標籤
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // 可以是普通文本
                singleLine = true,
                isError = authResultState is AuthResult.Error // 如果有錯誤，顯示錯誤狀態
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 密碼輸入框
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密碼 (Password)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "隱藏密碼" else "顯示密碼"

                    IconButton(onClick = viewModel::togglePasswordVisibility) { // 調用 ViewModel 中的方法
                        Icon(imageVector = image, description)
                    }
                },
                isError = authResultState is AuthResult.Error
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 錯誤信息顯示區域
            if (authResultState is AuthResult.Error) {
                Text(
                    text = authResultState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // 預留空間，防止按鈕跳動
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 登錄按鈕 (根據狀態禁用或顯示進度)
            Button(
                onClick = viewModel::signIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = authResultState != AuthResult.Loading // 加載時禁用按鈕
            ) {
                if (authResultState == AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary // 進度條顏色
                    )
                } else {
                    Text("登錄")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 跳轉到註冊頁
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline // 可選
                    )
                ) {
                    pushLink(
                        LinkAnnotation.Clickable(
                            tag = "NAVIGATE_REGISTER",
                            linkInteractionListener = { // <--- Lambda 不接收 'it'
                                println("Clicked Register Link (Tag: NAVIGATE_REGISTER)") // 可以打印固定文本
                                viewModel.resetAuthState()
                                onNavigateToRegister()
                            }
                        ))
                    append("還沒有賬戶？點此註冊")
                    pop()
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge
            )

        }
    }
}