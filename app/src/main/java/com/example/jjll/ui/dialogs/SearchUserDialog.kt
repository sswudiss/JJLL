package com.example.jjll.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.screens.contacts.ContactsUiState // 導入 UI State
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class) // For ListItem and TextField
@Composable
fun SearchUserDialog(
    uiState: ContactsUiState, // 接收來自 ViewModel 的狀態
    onQueryChange: (String) -> Unit, // 搜索查詢變更回調
    onAddContactClick: (userId: String) -> Unit, // 添加聯繫人按鈕點擊回調
    onDismiss: () -> Unit // 對話框關閉回調
) {
    // 使用 remember state 來管理 Snackbar 的顯示 (或臨時日誌)
    var feedbackMessage by remember { mutableStateOf<String?>(null) } // 可以改個更通用的名字

    // 當 addContactResult 變化時，更新 snackbarMessage
    LaunchedEffect(uiState.actionResult) { // <-- 監聽 actionResult
        feedbackMessage = uiState.actionResult // <-- 使用 actionResult
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ) // 點擊外部或返回鍵關閉
    ) {
        Surface( // 為對話框內容提供背景和形狀
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp) // 限制最大高度，防止內容過多撐滿屏幕
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                        verticalArrangement= Arrangement.Top,
                                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                // 標題和關閉按鈕
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "搜索用戶",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "關閉")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 搜索輸入框
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("輸入用戶名") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    placeholder = { Text("至少輸入 2 個字符") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 搜索結果或狀態顯示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 佔用剩餘空間
                ) {
                    when {
                        uiState.isSearching -> { // 顯示加載指示器
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        uiState.searchError != null -> { // 顯示錯誤信息
                            Text(
                                text = "搜索失敗: ${uiState.searchError}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        uiState.searchResults.isEmpty() && uiState.searchQuery.length >= 2 -> { // 顯示未找到結果
                            Text(
                                text = "未能找到用戶 '${uiState.searchQuery}'",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        uiState.searchResults.isNotEmpty() -> { // 顯示結果列表
                            LazyColumn {
                                // 使用接收 List 的 items 擴展函數  //import androidx.compose.foundation.lazy.items
                                items(
                                    items = uiState.searchResults, // <-- 傳遞 List<UserProfile>
                                    key = { userProfile -> userProfile.user_id } // <-- key lambda 接收 UserProfile
                                ) { user -> // <-- itemContent lambda 接收 UserProfile
                                    SearchResultItem(
                                        userProfile = user,
                                        onAddClick = { onAddContactClick(user.user_id) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    // 可以加一個初始狀態提示，如果需要的話
                    // else -> { Text("請輸入用戶名開始搜索", modifier = Modifier.align(Alignment.Center)) }
                }
            }

            // Snackbar 區域 (用於顯示添加結果)
            // 注意：Dialog 中顯示 Snackbar 可能行為不理想，更好的方式是在 MainScreen 的 Scaffold 中顯示
            // 這裡僅作演示，實際效果可能需要在 Dialog 外處理
            if (feedbackMessage != null) {
                LaunchedEffect(feedbackMessage) { // 當消息出現時顯示 Snackbar
                    // 這裡的實現依賴於是否有 ScaffoldState 可用
                    // 在 Dialog 內部直接顯示 Snackbar 比較 tricky
                    // 考慮通過 ViewModel 的 SharedFlow + MainScreen 的 ScaffoldState 實現
                    println("Snackbar TODO: $feedbackMessage ") // 臨時日誌
                    // 清除消息，防止重複顯示
                    // snackbarMessage = null // 需要更好的機制來控制顯示時間
                }
            }
        }
    }
}


// 搜索結果列表項
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(
    userProfile: UserProfile,
    onAddClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(userProfile.display_name ?: userProfile.username) },
        supportingContent = { Text("@${userProfile.username}") },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfile.avatar_url)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .fallback(R.drawable.ic_avatar_placeholder)
                    .build(),
                contentDescription = "${userProfile.username} 的頭像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = { // 在列表項尾部添加按鈕
            IconButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "添加聯繫人")
            }
        }
    )
}