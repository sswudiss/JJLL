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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.theme.JJLLTheme


@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatClick: (userId: String, userName: String?) -> Unit // 修改回調，傳遞用戶名用於詳情頁標題
) {
    val uiState by viewModel.uiState

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "加載列表失敗: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadChatPartners() }) {
                        Text("重試")
                    }
                }
            }
            uiState.chatPartners.isEmpty() -> {
                Text(
                    text = "還沒有開始任何聊天。\n從通訊錄中找人開始聊天吧！",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.chatPartners, key = { it.user_id }) { partner ->
                        // 可以復用 ContactItem，或創建一個 ChatListItem
                        ChatListItem(
                            userProfile = partner,
                            // TODO: 後期顯示真實的 lastMessage 和 timestamp
                            lastMessage = "點擊開始聊天...", // 佔位符
                            timestamp = "", // 佔位符
                            unreadCount = 0, // 佔位符
                            onClick = {
                                val partnerName = partner.display_name ?: partner.username
                                onChatClick(partner.user_id, partnerName)
                            }
                        )
                        Divider(modifier = Modifier.padding(start = 72.dp)) // 左側縮進
                    }
                }
            }
        }
    }
}

// 聊天列表項 Composable (可以基於 ContactItem 修改)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListItem(
    userProfile: UserProfile,
    lastMessage: String,
    timestamp: String, // 後期改為合適的時間類型
    unreadCount: Int,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp), // 給 Item 自身加點水平邊距
        headlineContent = {
            Text(
                text = userProfile.display_name ?: userProfile.username,
                fontWeight = FontWeight.SemiBold, // 姓名加粗一些
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = lastMessage, // 顯示最後一條消息摘要
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant // 摘要顏色淺一點
            )
        },
        leadingContent = {
            // 可以考慮在頭像上加未讀消息紅點 (需要 Box 包裹)
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userProfile.avatar_url)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .fallback(R.drawable.ic_avatar_placeholder)
                        .build(),
                    contentDescription = "${userProfile.username} 的頭像",
                    modifier = Modifier.size(56.dp).clip(CircleShape), // 列表頭像可以大一點
                    contentScale = ContentScale.Crop
                )
                // 未讀紅點 (示例)
                if (unreadCount > 0) {
                    Badge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)) {
                        // Text("$unreadCount") // 顯示數字，如果空間夠的話
                    }
                }
            }
        },
        trailingContent = {
            // 顯示時間戳
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
fun ChatListScreenPreview_Empty() {
    JJLLTheme {
        Surface{
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "還沒有開始任何聊天。\n從通訊錄中找人開始聊天吧！",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun ChatListItemPreview() {
    JJLLTheme {
        val previewProfile = UserProfile("prev_id", "preview_user", "聊天對象預覽")
        Surface {
            ChatListItem(
                userProfile = previewProfile,
                lastMessage = "這是最後一條消息的預覽...",
                timestamp = "昨天",
                unreadCount = 3,
                onClick = {}
            )
        }
    }
}