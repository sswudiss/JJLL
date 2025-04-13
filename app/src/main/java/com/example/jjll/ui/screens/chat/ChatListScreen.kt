package com.example.jjll.ui.screens.chat


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R
import com.example.jjll.data.ChatListItem
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.navigation.AppDestinations


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = hiltViewModel() // 注入 ViewModel
) {
    // --- 狀態收集 ---
    val chatList by viewModel.chatList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("聊天") })
            // 可在此添加例如 "創建群聊" 等操作按鈕
            /*
            actions = {
                IconButton(onClick = { /* TODO: Navigate to create group screen */ }) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "創建群聊")
                }
            }
            */
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 應用 Scaffold 的內邊距
        ) {
            when {
                // 正在加載時顯示指示器
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 出錯時顯示錯誤信息
                error != null -> {
                    Text(
                        text = "錯誤: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                // 列表為空時顯示提示信息
                chatList.isEmpty() -> {
                    Text(
                        "暫無聊天記錄，快去通訊錄找好友聊天吧！", // 提供更友好的提示
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                // 顯示聊天列表
                else -> {
                    ChatListContent(
                        chatList = chatList,
                        onChatItemClick = { userProfile ->
                            // 導航到聊天詳情頁
                            navController.navigate(
                                AppDestinations.ChatDetail.buildRoute(
                                    userId = userProfile.userId,
                                    // 提供默認值或處理 username 為 null 的情況
                                    username = userProfile.username ?: "用戶"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListContent(
    chatList: List<ChatListItem>,
    onChatItemClick: (UserProfile) -> Unit // 回調點擊事件，傳遞對方 Profile
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 0.dp) // 列表上下邊距通過 Divider 控制
    ) {
        items(chatList, key = { it.partnerProfile.userId }) { chatItem -> // 使用對方用戶 ID 作為 key
            ChatListItemView(
                chatItem = chatItem,
                onClick = { onChatItemClick(chatItem.partnerProfile) } // 點擊時回調
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) // 添加分隔線
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // ListItem 需要
@Composable
fun ChatListItemView(
    chatItem: ChatListItem,
    onClick: () -> Unit
) {
    ListItem( // 調用 Material 3 的 ListItem
        modifier = Modifier.clickable(onClick = onClick),
        // --- 修改這裡：使用 headlineContent ---
        headlineContent = {
            Text(
                text = chatItem.partnerProfile.displayName ?: chatItem.partnerProfile.username ?: "未知用戶",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // --- 修改這裡：使用 supportingContent ---
        supportingContent = {
            Text(
                text = chatItem.lastMessage?.content ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        // --- leadingContent 名稱正確 ---
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(chatItem.partnerProfile.avatarURL)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_placeholder_person)
                    .error(R.drawable.ic_placeholder_person)
                    .build(),
                contentDescription = "對話夥伴頭像",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        // --- trailingContent 名稱正確 ---
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestampRelative(chatItem.lastMessage?.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 可選：未讀標記
            }
        }
        // --- 其他 ListItem 參數，如 colors, tonalElevation 等可以根據需要添加 ---
        // colors = ListItemDefaults.colors(...)
        // tonalElevation = ListItemDefaults.Elevation // 默認可能已有
    )
}

// 輔助函數：格式化相對時間戳 (需要您自己實現或引入庫)
fun formatTimestampRelative(timestamp: String?): String {
    // 實現將 ISO 8601 字符串轉換為相對時間，例如：
    // "剛剛", "5分鐘前", "昨天 10:30", "10/27" 等
    // 這通常需要日期時間處理庫 (如 kotlinx-datetime 或 ThreeTenABP)
    return timestamp?.take(10) ?: "" // 極簡示例
}

// 確保您在 res/drawable 文件夾下有名為 ic_placeholder_person.xml 的資源文件