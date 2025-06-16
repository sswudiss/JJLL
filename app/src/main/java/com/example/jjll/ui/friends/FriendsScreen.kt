package com.example.jjll.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.data.model.FriendRequest
import com.example.jjll.data.model.Friendship
import com.example.jjll.ui.navigation.Screen
import com.example.jjll.viewmodel.FriendsViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoadingChat by viewModel.isLoadingChat.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)

    // 監聽來自 ViewModel 的導航事件
    // LaunchedEffect 的 key 設置為 Unit，確保這個協程只在 Composable 首次進入時啟動
    LaunchedEffect(Unit) {
        viewModel.navigateToChat.collect { chatId ->
            navController.navigate(Screen.createChatDetailRoute(chatId))
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.SEARCH_USERS) }) {
                Icon(Icons.Default.Add, contentDescription = "添加好友")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.loadFriendsAndRequests() }
            ) {
                // 主內容區域
                if (uiState.isLoading && uiState.friends.isEmpty() && uiState.friendRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error ?: "發生未知錯誤",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // 好友請求部分
                        if (uiState.friendRequests.isNotEmpty()) {
                            item {
                                ListHeader("好友請求")
                            }
                            items(uiState.friendRequests, key = { "request-${it.id}" }) { request ->
                                FriendRequestItem(
                                    request = request,
                                    onAccept = { viewModel.acceptFriendRequest(request.id) },
                                    onDecline = { viewModel.declineFriendRequest(request.id) }
                                )
                            }
                        }

                        // 好友列表部分
                        item {
                            ListHeader("我的好友 (${uiState.friends.size})")
                        }
                        if (uiState.friends.isEmpty()) {
                            // 只有在請求列表也為空時才顯示“沒有好友”的提示
                            if (uiState.friendRequests.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxHeight(0.5f) // 佔用剩餘空間的一半
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("還沒有好友，點擊右下角按鈕去添加吧！")
                                    }
                                }
                            }
                        } else {
                            items(uiState.friends, key = { "friend-${it.id}" }) { friendship ->
                                FriendItem(
                                    friendship = friendship,
                                    onClick = { viewModel.onFriendClick(friendship) }
                                )
                            }
                        }
                    }
                }
            }

            // 如果正在獲取 chatId，顯示一個覆蓋整個屏幕的加載指示器
            if (isLoadingChat) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false, onClick = {}), // 阻止下層點擊
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(request.requesterProfile?.avatarUrl).crossfade(true).build(),
            contentDescription = "Requester Avatar",
            placeholder = rememberVectorPainter(image = Icons.Default.AccountCircle),
            error = rememberVectorPainter(image = Icons.Default.AccountCircle),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.requesterProfile?.username ?: "未知用戶", fontWeight = FontWeight.SemiBold)
            if (!request.message.isNullOrBlank()) {
                Text(request.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAccept, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("同意") }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onDecline, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("拒絕") }
    }
}

@Composable
private fun FriendItem(
    friendship: Friendship,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(friendship.friendProfile?.avatarUrl).crossfade(true).build(),
            contentDescription = "Friend Avatar",
            placeholder = rememberVectorPainter(image = Icons.Default.AccountCircle),
            error = rememberVectorPainter(image = Icons.Default.AccountCircle),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = friendship.friendProfile?.username ?: "未知好友",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}