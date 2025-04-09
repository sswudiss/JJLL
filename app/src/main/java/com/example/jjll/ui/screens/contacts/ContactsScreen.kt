package com.example.jjll.ui.screens.contacts

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.theme.JJLLTheme
import com.example.jjll.R


private const val TAG = "ContactsScreen" // 添加 Log Tag

@OptIn(ExperimentalMaterial3Api::class) // For Scaffold, SnackbarHostState
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onContactClick: (userId: String) -> Unit, // 點擊聯繫人回調
    // 將 SnackbarHostState 從 MainScreen 傳入是更好的實踐
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // 從 ViewModel 觀察 UI 狀態
    val uiState by viewModel.uiState

    // 使用 LaunchedEffect 監聽 actionResult 以顯示 Snackbar
    // key 設置為 uiState.actionResult，當它從 null 變為有值時觸發
    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let { message ->
            Log.d(TAG, "Showing snackbar for actionResult: $message")
            try {
                // 顯示 Snackbar
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            } finally {
                // 無論成功或失敗，顯示後都清除消息，防止重複顯示
                viewModel.clearActionResult()
            }
        }
    }

    // 主體佈局：使用 Column 組合請求區和聯繫人區
    Column(modifier = Modifier.fillMaxSize()) {

        // --- 待處理請求區域 ---
        // 加載或錯誤狀態只顯示文本提示或小型指示器，不阻塞聯繫人列表
        if (uiState.isLoadingPending) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically){
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在加載好友請求...", style = MaterialTheme.typography.bodySmall)
            }
        } else if (uiState.pendingError != null) {
            Text(
                text = "加載好友請求失敗", // 簡化錯誤提示
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            // 可以添加小型重試按鈕
            // TextButton(onClick = { viewModel.loadPendingRequests() }){ Text("重試請求")}
        } else if (uiState.pendingRequests.isNotEmpty()) {
            // 如果有待處理請求，則顯示它們
            Column {
                Text(
                    "好友請求",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp) // 調整邊距
                )
                uiState.pendingRequests.forEach { request ->
                    PendingRequestItem( // 顯示每個請求
                        pendingRequest = request,
                        onAcceptClick = {
                            Log.d(TAG, "Accept button clicked for request id: ${request.contactId}")
                            viewModel.acceptContactRequest(request.contactId)
                        },
                        onRejectClick = {
                            Log.d(TAG, "Reject button clicked for request id: ${request.contactId}")
                            viewModel.rejectContactRequest(request.contactId)
                        }
                    )
                    // Divider(modifier = Modifier.padding(start = 16.dp)) // Pending Item 內部可以不加 Divider
                }
                Divider(thickness = 8.dp, modifier = Modifier.padding(top = 8.dp)) // 用厚 Divider 分隔區域
            }
        }
        // 如果沒有待處理請求，則不顯示任何內容，直接進入聯繫人列表區域

        // --- 已接受聯繫人區域 ---
        // 添加 Log 檢查進入此區域時的狀態
        Log.d(TAG, "Rendering Accepted Contacts Area: isLoading=${uiState.isLoadingContacts}, error=${uiState.contactsError}, count=${uiState.contacts.size}")

        Box(modifier = Modifier.weight(1f)) { // 佔用剩餘空間
            when {
                // 只在首次加載聯繫人且列表為空時顯示全局加載
                uiState.isLoadingContacts && uiState.contacts.isEmpty() && uiState.contactsError == null -> {
                    Log.d(TAG, "Displaying loading indicator for contacts.")
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 加載出錯
                uiState.contactsError != null -> {
                    Log.d(TAG, "Displaying contacts error message.")
                    Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加載聯繫人失敗: ${uiState.contactsError}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadAcceptedContacts() }) { Text("重試") }
                    }
                }
                // 成功加載但列表為空
                uiState.contacts.isEmpty() && !uiState.isLoadingContacts -> {
                    Log.d(TAG, "Displaying empty contacts message.")
                    Text("你的通訊錄是空的。\n點擊頂部搜索按鈕添加聯繫人吧！", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }
                // 成功加載且列表不為空
                else -> {
                    Log.d(TAG, "Displaying contacts list with ${uiState.contacts.size} items.")
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.contacts, key = { it.user_id }) { contact ->
                            ContactItem(
                                userProfile = contact,
                                onClick = {
                                    Log.d(TAG, "Contact item clicked: ${contact.user_id}")
                                    onContactClick(contact.user_id)
                                }
                            )
                        }
                    }
                }
            }
            // 如果正在下拉刷新，可以疊加顯示刷新指示器
            // PullRefreshIndicator(...)
        } // Box 結束 (已接受聯繫人區域)
    } // Column 結束 (整體佈局)

    // 如果 SnackbarHostState 由外部傳入，這裡就不需要 SnackbarHost
    // Box(Modifier.fillMaxSize()) {
    //     SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    // }
}


// --- 待處理請求列表項 ---
//於顯示單個待處理的好友請求，包含發送者信息以及接受和拒絕按鈕。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingRequestItem(
    pendingRequest: PendingRequest, // 接收 PendingRequest 數據
    onAcceptClick: () -> Unit,      // 接受按鈕點擊回調
    onRejectClick: () -> Unit       // 拒絕按鈕點擊回調
) {
    ListItem(
        // headlineContent: 顯示發送者名稱
        headlineContent = {
            Text(
                text = pendingRequest.senderProfile.display_name ?: pendingRequest.senderProfile.username,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // supportingContent: 顯示發送者 @username
        supportingContent = {
            Text(
                text = "@${pendingRequest.senderProfile.username}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // leadingContent: 顯示發送者頭像
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(pendingRequest.senderProfile.avatar_url)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .fallback(R.drawable.ic_avatar_placeholder)
                    .build(),
                contentDescription = "${pendingRequest.senderProfile.username} 的頭像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        // trailingContent: 放置接受和拒絕按鈕
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 接受按鈕 (綠色 Check)
                IconButton(
                    onClick = onAcceptClick,
                    modifier = Modifier.size(32.dp) // 可以稍微調整按鈕大小
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "接受好友請求",
                        tint = Color(0xFF4CAF50) // 使用綠色調
                    )
                }
                // 間隔
                Spacer(modifier = Modifier.width(8.dp))
                // 拒絕/忽略按鈕 (紅色 Close)
                IconButton(
                    onClick = onRejectClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "忽略好友請求",
                        tint = MaterialTheme.colorScheme.error // 使用主題的錯誤顏色 (通常是紅色)
                    )
                }
            }
        },
        modifier = Modifier.padding(vertical = 4.dp) // 給列表項本身一點垂直邊距
    )
}



// --- 已接受聯繫人列表項 ---
@OptIn(ExperimentalMaterial3Api::class) // ListItem 需要此注解
@Composable
private fun ContactItem(
    userProfile: UserProfile, // 接收 UserProfile 數據
    onClick: () -> Unit       // 接收點擊事件的回調
) {
    // 使用 Column 包裹 ListItem 和 Divider (可選)
    Column {
        ListItem(
            modifier = Modifier
                .fillMaxWidth() // 佔滿寬度
                .clickable(onClick = onClick), // 使整行可點擊，觸發外部傳入的 onClick
            // 主要文本內容：優先顯示 displayName，否則顯示 username
            headlineContent = {
                Text(
                    text = userProfile.display_name ?: userProfile.username,
                    style = MaterialTheme.typography.bodyLarge, // 設置文本樣式
                    maxLines = 1, // 最多顯示一行
                    overflow = TextOverflow.Ellipsis // 超長時用省略號
                )
            },
            // 次要文本內容：顯示 @username
            supportingContent = {
                Text(
                    text = "@${userProfile.username}",
                    style = MaterialTheme.typography.bodyMedium, // 稍小的樣式
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            // 列表項開頭的內容：顯示頭像
            leadingContent = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userProfile.avatar_url) // 加載頭像 URL
                        .crossfade(true) // 淡入效果
                        .placeholder(R.drawable.ic_avatar_placeholder) // 佔位圖
                        .error(R.drawable.ic_avatar_placeholder)       // 加載錯誤圖
                        .fallback(R.drawable.ic_avatar_placeholder)    // URL 為 null 時的圖
                        .build(),
                    contentDescription = "${userProfile.username} 的頭像", // 無障礙描述
                    modifier = Modifier
                        .size(40.dp) // 設置頭像大小
                        .clip(CircleShape), // 裁剪成圓形
                    contentScale = ContentScale.Crop // 圖像裁剪方式
                )
            }
            // trailingContent = { } // 可選：在這裡添加尾部圖標或按鈕
        )
        // 可選：在每個 ListItem 下方添加分隔線
        // Divider(modifier = Modifier.padding(start = 72.dp)) // 左側縮進以對齊文本（40dp 頭像 + 16dp 間距 + 16dp 文本區間距）
    }
}



// --- ContactItem 的預覽 ---
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ContactItemPreview() {
    JJLLTheme {
        val previewProfile = UserProfile(
            user_id = "preview_id",
            username = "preview_contact_username_very_long_to_test_ellipsis",
            display_name = "聯繫人名稱預覽",
            avatar_url = null // 或者提供一個測試 URL: "https://picsum.photos/id/237/50/50"
        )
        Surface { // 添加 Surface 以應用背景色
            ContactItem(userProfile = previewProfile, onClick = {})
        }
    }
}



// --- PendingRequestItem 的預覽 ---
@Preview(showBackground = true, widthDp = 360)
@Composable
fun PendingRequestItemPreview() {
    JJLLTheme {
        val previewSender = UserProfile(
            user_id = "sender_preview_id",
            username = "sender_username_long",
            display_name = "請求發送者名稱預覽",
            avatar_url = null
        )
        val previewRequest = PendingRequest(contactId = 1L, senderProfile = previewSender)
        Surface { // 添加 Surface 以應用背景色
            PendingRequestItem(
                pendingRequest = previewRequest,
                onAcceptClick = {},
                onRejectClick = {}
            )
        }
    }
}