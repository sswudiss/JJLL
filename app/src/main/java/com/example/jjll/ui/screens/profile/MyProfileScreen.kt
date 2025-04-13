package com.example.jjll.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R
import com.example.jjll.data.UserProfile
import androidx.compose.material.icons.filled.Edit // 示例圖標
import androidx.compose.material.icons.filled.ExitToApp


@OptIn(ExperimentalMaterial3Api::class) // 需要用於 Scaffold, TopAppBar 等
@Composable
fun MyProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(), // 注入 ViewModel
    onLogout: () -> Unit // 從 MainScreen 傳入登出回調
) {
    // --- 狀態收集 ---
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // (可選) 如果需要在進入屏幕時觸發加載
    // LaunchedEffect(Unit) {
    //     viewModel.loadUserProfile() // 假設 ViewModel 有此方法
    // }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    // 示例：添加編輯按鈕
                    IconButton(onClick = { /* TODO: 導航到編輯個人資料頁 */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "編輯資料")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 應用 Scaffold 內邊距
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Text(
                        "錯誤: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                userProfile != null -> {
                    // 顯示用戶資料內容
                    ProfileContent(
                        userProfile = userProfile!!, // 已確認非 null
                        onLogoutClick = {
                            // 觸發 ViewModel 中的登出邏輯（如果有的話）
                            // viewModel.logout()
                            // 調用外部回調以處理導航
                            onLogout()
                        }
                    )
                }

                else -> {
                    // 用戶資料為 null 且未在加載也無錯誤 (可能初始化未完成或加載失敗但未設置 error)
                    Text(
                        "無法加載用戶資料",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    userProfile: UserProfile,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // 將登出按鈕推到底部
    ) {
        // 上半部分：用戶信息
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 頭像
            // 假設您在 drawable 文件夾下有名為 ic_placeholder_person.xml 的矢量圖資源
// (您可以使用 Android Studio 的 Vector Asset Studio 創建一個)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfile.avatarURL) // 使用 avatarURL
                    .crossfade(true) // 淡入效果
                    .placeholder(R.drawable.ic_avatar_placeholder) // 設置佔位符 Drawable 資源
                    .error(R.drawable.ic_avatar_placeholder) // 設置錯誤時顯示的 Drawable 資源
                    .build(),
                contentDescription = "用戶頭像",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape), // 圓形裁剪
                contentScale = ContentScale.Crop // 裁剪方式
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 顯示名稱 (如果 displayName 為 null，則顯示 username)
            Text(
                text = userProfile.displayName ?: userProfile.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 用戶名 (如果 displayName 和 username 不同，可以顯示)
            if (userProfile.displayName != null && userProfile.displayName != userProfile.username) {
                Text(
                    text = "@${userProfile.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 使用次要顏色
                )
            } else if (userProfile.displayName == null) {
                // 如果沒有 displayName， username 已在上面顯示為主標題，這裡可以不顯示或顯示 ID
                // Text(text = "ID: ${userProfile.userId}", style = MaterialTheme.typography.bodySmall)
            }


            Spacer(modifier = Modifier.height(24.dp))

            // --- 其他可能的資料項 ---
            // ProfileInfoRow("郵箱:", userProfile.email ?: "-") // 假設 UserProfile 有 email
            // ProfileInfoRow("加入時間:", formatJoinDate(userProfile.createdAt)) // 需要格式化函數

        }

        // 底部：登出按鈕
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // 使用錯誤顏色突出登出
        ) {
            Icon(
                Icons.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("登出")
        }
    }
}

// 示例：用於顯示單行信息的輔助 Composable
@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp)) // 固定標籤寬度
        Text(value)
    }
}