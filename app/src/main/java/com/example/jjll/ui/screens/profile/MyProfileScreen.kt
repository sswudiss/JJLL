package com.example.jjll.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // 導入常用 Filled 圖標
import androidx.compose.material.icons.outlined.* // 導入常用 Outlined 圖標
import androidx.compose.material3.* // 導入 Material 3 組件
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R // 確保 R 文件已導入
import com.example.jjll.data.UserProfile

private const val TAG = "MyProfileScreen"

@Composable
fun MyProfileScreen(
    // navController: NavHostController, // 登出導航由 ViewModel 事件觸發，暫不需要 Controller
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState

    // 登出確認對話框
    if (uiState.showLogoutConfirmDialog) {
        LogoutConfirmDialog(
            onConfirm = viewModel::confirmLogout,
            onDismiss = viewModel::cancelLogout
        )
    }

    // 主體內容佈局
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 使內容可滾動
    ) {
        // 根據狀態顯示不同內容
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "加載失敗: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            uiState.userProfile != null -> {
                // 加載成功，顯示用戶資料和設置項
                ProfileContent(
                    userProfile = uiState.userProfile!!, // 確定非空
                    onLogoutClick = viewModel::requestLogout, // 觸發請求登出
                    onEditAvatarClick = { Log.d(TAG,"Edit Avatar Clicked (Placeholder)") /* TODO */},
                    onEditNameClick = { Log.d(TAG,"Edit Name Clicked (Placeholder)") /* TODO */},
                    onChangePasswordClick = { Log.d(TAG,"Change Password Clicked (Placeholder)") /* TODO */}
                )
            }
            else -> {
                // 理論上不應到達這裡，除非初始狀態異常
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("未能加載用戶資料")
                }
            }
        }
    }
}

// 用戶資料和設置項目的實際佈局
@Composable
private fun ProfileContent(
    userProfile: UserProfile,
    onLogoutClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    // 可以添加其他點擊回調
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. 頭像和名稱區域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 頭像
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfile.avatar_url)
                    .crossfade(true) // 淡入效果
                    .placeholder(R.drawable.ic_avatar_placeholder) // 使用你創建的佔位圖
                    .error(R.drawable.ic_avatar_placeholder)
                    .fallback(R.drawable.ic_avatar_placeholder)
                    .build(),
                contentDescription = "用戶頭像",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { onEditAvatarClick() }, // 頭像區域可點擊
                contentScale = ContentScale.Crop // 裁剪以適應形狀
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 名稱
            Text(
                text = userProfile.display_name ?: userProfile.username, // 優先顯示 display_name
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f) // 佔用剩餘空間
                    .clickable { onEditNameClick() } // 名稱區域可點擊
            )
        }

        // 2. 功能列表區域
        HorizontalDivider() // 分隔線

        SettingItem(
            icon = Icons.Outlined.Lock,
            title = "修改密碼",
            onClick = onChangePasswordClick
        )
        SettingItem(
            icon = Icons.Outlined.TextFields,
            title = "設置字體大小",
            onClick = { Log.d(TAG, "Set Font Size Clicked (Placeholder)") /* TODO */ }
        )
        SettingItem(
            icon = Icons.Outlined.Language,
            title = "多國語言選擇",
            onClick = { Log.d(TAG, "Select Language Clicked (Placeholder)") /* TODO */ }
        )
        SettingItem(
            icon = Icons.Outlined.Info,
            title = "關於 App",
            onClick = { Log.d(TAG, "About App Clicked (Placeholder)") /* TODO */ }
        )

        HorizontalDivider()

        // 佔位符將登出按鈕推到底部（如果頁面內容不夠長）
        Spacer(modifier = Modifier.weight(1f))

        // 3. 登出按鈕
        Button(
            onClick = onLogoutClick, // 調用 ViewModel 觸發登出流程
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error, // 紅色背景
                contentColor = MaterialTheme.colorScheme.onError // 白色文字
            )
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text("登出賬號")
        }
    }
}

// 可重用的設置列表項 Composable
@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 使整行可點擊
            .padding(horizontal = 16.dp, vertical = 16.dp), // 調整 padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Title 已提供描述
            tint = MaterialTheme.colorScheme.primary // 可以設置圖標顏色
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // 佔滿剩餘寬度
        )
        // 可以考慮在右側加一個 > 圖標
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// 登出確認對話框
@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, // 點擊外部或返回鍵時觸發
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("確認登出") },
        text = { Text("您確定要登出賬號嗎？如果忘記用戶名和密碼，將可能無法再次登錄。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("確認登出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}