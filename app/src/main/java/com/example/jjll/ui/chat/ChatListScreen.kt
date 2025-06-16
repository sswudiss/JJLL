package com.example.jjll.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.data.model.Chat
import com.example.jjll.viewmodel.ChatListViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatClick: (Chat) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.loadChatList() }
    ) {
        when {
            // 加載中 (初始加載時顯示，下拉刷新由 SwipeRefresh 的指示器處理)
            uiState.isLoading && uiState.chats.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // CircularProgressIndicator() // SwipeRefresh 已經有指示器了
                    Text("正在加載聊天...")
                }
            }
            // 處理錯誤
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.error ?: "發生未知錯誤",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            // 處理空列表
            uiState.chats.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "還沒有聊天記錄。\n去聯繫人頁面開始聊天吧！",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            // 顯示聊天列表
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.chats, key = { it.id }) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 對方頭像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(chat.otherParticipantProfile?.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "${chat.otherParticipantProfile?.username}'s avatar",
            placeholder = rememberVectorPainter(image = Icons.Default.AccountCircle),
            error = rememberVectorPainter(image = Icons.Default.AccountCircle),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 包含名字和時間戳的行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.otherParticipantProfile?.username ?: "未知用戶",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false) // 避免名字過長時將時間戳擠走
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(chat.lastMessage?.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 最後一條消息預覽
            Text(
                text = chat.lastMessage?.content ?: "還沒有消息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A helper function to format the ISO timestamp string into a user-friendly format.
 */
@Composable
private fun formatTimestamp(timestamp: String?): String {
    if (timestamp == null) return ""

    // 使用 remember 來避免在每次重組時重新創建 Formatter
    val todayFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val otherDayFormatter = remember { DateTimeFormatter.ofPattern("MM/dd") }

    return try {
        val instant = Instant.parse(timestamp)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())

        when {
            // 今天
            ChronoUnit.DAYS.between(localDateTime.toLocalDate(), now.toLocalDate()) == 0L -> {
                localDateTime.format(todayFormatter)
            }
            // 昨天
            ChronoUnit.DAYS.between(localDateTime.toLocalDate(), now.toLocalDate()) == 1L -> {
                "昨天"
            }
            // 更早
            else -> {
                localDateTime.format(otherDayFormatter)
            }
        }
    } catch (e: Exception) {
        // 如果時間戳格式有問題，返回空字符串或一個錯誤標記
        ""
    }
}