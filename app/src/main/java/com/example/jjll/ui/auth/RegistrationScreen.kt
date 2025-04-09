package com.example.jjll.ui.auth

import androidx.compose.foundation.layout.*
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
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // 收集 ViewModel 的狀態
    val username = viewModel.username
    val password = viewModel.password
    val confirmPassword = viewModel.confirmPassword
    val passwordVisible = viewModel.passwordVisible
    val confirmPasswordVisible = viewModel.confirmPasswordVisible
    val authResultState = viewModel.authResultState

    // 監聽導航事件
    LaunchedEffect(key1 = viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateToMain -> onRegisterSuccess()
                // NavigationEvent.NavigateToLogin -> onNavigateToLogin() // 由按鈕觸發
                else -> {} // RegistrationScreen 只關心註冊成功
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("創建您的賬戶", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            // 用戶名輸入框
            OutlinedTextField(
                value = username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("用戶名 (Username)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                // 根據錯誤消息判斷 isError，消息中不再包含“郵件”
                isError = authResultState is AuthResult.Error && authResultState.message.contains(
                    "用戶名"
                )
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
                    val image =
                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = image,
                            contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼"
                        )
                    }
                },
                isError = authResultState is AuthResult.Error && (authResultState as AuthResult.Error).message.contains(
                    "密碼"
                ) && !(authResultState as AuthResult.Error).message.contains("一致")
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 確認密碼輸入框
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("確認密碼 (Confirm Password)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image =
                        if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = image,
                            contentDescription = if (confirmPasswordVisible) "隱藏密碼" else "顯示密碼"
                        )
                    }
                },
                isError = authResultState is AuthResult.Error && (authResultState as AuthResult.Error).message.contains(
                    "一致"
                )
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
                Spacer(modifier = Modifier.height(24.dp)) // 佔位
            }

            // 註冊按鈕
            Button(
                onClick = viewModel::signUp,
                modifier = Modifier.fillMaxWidth(),
                enabled = authResultState != AuthResult.Loading
            ) {
                if (authResultState == AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("註冊")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 跳轉到登錄頁
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline // 可選
                    )
                ) {
                    pushLink(
                        LinkAnnotation.Clickable(
                            tag = "NAVIGATE_LOGIN",
                            linkInteractionListener = { // <--- Lambda 不接收 'it'
                                println("Clicked Login Link (Tag: NAVIGATE_LOGIN)") // 可以打印固定文本
                                viewModel.resetAuthState()
                                onNavigateToLogin()
                            }
                        ))
                    append("已有賬戶？點此登錄")
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