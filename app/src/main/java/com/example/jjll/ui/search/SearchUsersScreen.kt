package com.example.jjll.ui.search // 假設的文件路徑

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.data.model.Profile
import com.example.jjll.viewmodel.RequestSentStatus
import com.example.jjll.viewmodel.SearchUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersScreen(
    navController: NavController,
    viewModel: SearchUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 用於顯示發送請求後的 Snackbar 提示
    LaunchedEffect(uiState.requestSentStatus) {
        when (uiState.requestSentStatus) {
            RequestSentStatus.SUCCESS -> snackbarHostState.showSnackbar("好友請求已發送！")
            RequestSentStatus.FAILURE -> snackbarHostState.showSnackbar("發送請求失敗，請重試。")
            RequestSentStatus.ALREADY_SENT -> snackbarHostState.showSnackbar("你已發送過好友請求。")
            null -> {} // Do nothing
        }
    }

    // 狀態變量，用於控制對話框的顯示和選中的用戶
    var showDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("添加好友") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 搜索輸入框
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("搜索用戶名...") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            // 搜索結果區域
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (uiState.searchPerformed && uiState.searchResults.isEmpty()) {
                    Text(
                        text = "沒有找到匹配的用戶。",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.searchResults, key = { it.userId }) { profile ->
                            SearchResultItem(
                                profile = profile,
                                onAddClick = {
                                    selectedProfile = profile
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 發送好友請求的對話框
    if (showDialog && selectedProfile != null) {
        SendRequestDialog(
            profile = selectedProfile!!,
            onDismiss = { showDialog = false },
            onSend = { message ->
                viewModel.sendFriendRequest(selectedProfile!!.userId, message)
                showDialog = false
            }
        )
    }
}

@Composable
fun SearchResultItem(
    profile: Profile,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User Avatar",
                // 使用 rememberVectorPainter 將 ImageVector 轉為 Painter
                placeholder = rememberVectorPainter(image = Icons.Default.AccountCircle),
                error = rememberVectorPainter(image = Icons.Default.AccountCircle),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.displayName ?: profile.username ?: "未知用戶",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    profile.username ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddClick) {
                Text("添加")
            }
        }
    }
}


@Composable
fun SendRequestDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSend: (message: String) -> Unit
) {
    var message by remember { mutableStateOf("你好，我是...") } // 可以從自己的 Profile 中獲取名字

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("向 ${profile.username} 發送好友請求") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("備註信息 (可選)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSend(message) }) { Text("發送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}