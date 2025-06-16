package com.example.jjll.ui.auth // 替換成你的包名

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.jjll.ui.navigation.Screen
import com.example.jjll.viewmodel.AuthViewModel


@Composable
fun BlinkingText(
    text: String,
    targetColor: Color = MaterialTheme.colorScheme.error,
    initialColor: Color = Color.Transparent, // 或者你希望的初始顏色
    durationMillis: Int = 2500 // 閃爍一次的總時長 (亮 -> 暗 -> 亮)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blinkingTextTransition")
    val color by infiniteTransition.animateColor(
        initialValue = initialColor,
        targetValue = targetColor,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse // 這會讓顏色在 initialValue 和 targetValue 之間來回切換
        ), label = "blinkingTextColor"
    )

    Text(
        text = text,
        color = color
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var username  by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    // 如果已註冊並登錄，導航到 Home (Supabase 註冊後默認會登錄)
    LaunchedEffect(authState) {
        if (authState.isAuthenticated) {
            navController.navigate(Screen.HOME) {
                popUpTo(Screen.SIGN_UP) { inclusive = true } // 清除註冊頁面
                popUpTo(Screen.LOGIN) { inclusive = true } // 同時也可能清掉登錄頁
            }
        }
    }

    // 判斷註冊按鈕是否應該啟用
    val isSignUpButtonEnabled = username.isNotBlank() &&
            password.isNotEmpty() &&
            password == confirmPassword && // 確保密碼匹配
            !authState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("JJLL (兔子) - 註冊") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("創建你的賬戶", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(18.dp))

            // 使用 BlinkingText
            BlinkingText(
                text = "用戶名和密碼沒有找回功能，請牢記！",
                targetColor = MaterialTheme.colorScheme.error,
                // 你可以選擇一個與背景色相近的顏色作為 initialColor，
                // 或者直接使用 Color.Transparent 使其完全"消失"
                initialColor = MaterialTheme.colorScheme.surface // 假設背景是 surface 顏色
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.trim() }, // 可以 trim 一下，防止用戶輸入空格
                label = { Text("用戶名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密碼") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("確認密碼") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = password != confirmPassword && confirmPassword.isNotEmpty()
            )
            if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                Text("密碼不匹配", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            authState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(39.dp))

            Button(
                onClick = {
                    // isSignUpButtonEnabled 已經包含了對 !authState.isLoading 的檢查
                    if (isSignUpButtonEnabled) {
                        authViewModel.signUpWithUsername(username, password)
                    }
                },
                enabled = isSignUpButtonEnabled, // <--- 關鍵修改
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("創建賬戶")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = {
                authViewModel.clearAuthError()
                navController.navigate(Screen.LOGIN) {
                popUpTo(Screen.SIGN_UP) { inclusive = true }
            } }) {
                Text("已經有賬戶了？去登錄")
            }
        }
    }
}