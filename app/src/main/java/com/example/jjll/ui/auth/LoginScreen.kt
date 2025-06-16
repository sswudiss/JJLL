package com.example.jjll.ui.auth // 替換成你的包名

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.jjll.ui.navigation.Screen
import com.example.jjll.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel() // 注入 ViewModel
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState() // 觀察認證狀態

    // 如果已登錄，導航到 Home
    LaunchedEffect(authState) {
        if (authState.isAuthenticated) {
            navController.navigate(Screen.HOME) {
                popUpTo(Screen.LOGIN) { inclusive = true } // 清除登錄頁面
            }
        }
    }

    // 判斷登錄按鈕是否應該啟用
    val isLoginButtonEnabled =
        username.isNotBlank() && password.isNotEmpty() && !authState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("JJLL (兔子) - 登錄") })
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
            Text("歡迎回來！", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(46.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.trim() },  // trim() 確保用戶輸入的純空格被視為空
                label = { Text("用戶名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密碼") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            authState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(39.dp))

            Button(
                onClick = {
                    // 雖然按鈕已禁用，但作為保險，onClick 內部也可以再判斷一次
                    if (isLoginButtonEnabled) { // 確保 isLoading 時也不觸發
                        authViewModel.signInWithUsername(username, password)
                    }
                },
                enabled = isLoginButtonEnabled, // <--- 關鍵修改
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("登錄")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = {
                authViewModel.clearAuthError()
                navController.navigate(Screen.SIGN_UP)
            }) {
                Text("還沒有賬戶？去註冊")
            }
        }
    }
}