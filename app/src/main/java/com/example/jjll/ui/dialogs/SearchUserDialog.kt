package com.example.jjll.ui.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jjll.data.ContactStatus
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import com.example.jjll.ui.screens.contacts.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SearchUserDialog" // Tag for logging


//搜尋用戶對話框
@Composable
fun SearchUserDialog(
    onDismiss: () -> Unit,
    profileRepository: ProfileRepository,
    contactRepository: ContactRepository,
    authRepository: AuthRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    // Fetch current user ID once when the dialog enters composition
    LaunchedEffect(Unit) {
        try {
            currentUserId = authRepository.getCurrentUserId()
            Log.d(TAG, "Current User ID: $currentUserId")
            if (currentUserId == null) {
                error = "无法获取当前用户信息，请重新登录。"
                Log.e(TAG, "Failed to get current user ID.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID", e)
            error = "获取用户信息时出错: ${e.message}"
        }
    }

    // Debounced search effect
    LaunchedEffect(searchQuery) {
        searchJob?.cancel() // Cancel previous search job if query changes quickly
        if (searchQuery.isBlank() || searchQuery.length < 2) { // Only search if query is not blank and has min length (e.g., 2)
            searchResults = emptyList()
            isSearching = false
            error = null // Clear error when query is cleared or too short
            return@LaunchedEffect
        }

        searchJob = coroutineScope.launch {
            delay(500L) // Debounce: wait 500ms after last keystroke
            isSearching = true
            error = null
            Log.d(TAG, "Starting search for query: '$searchQuery'")
            try {
                val results = profileRepository.searchProfiles(searchQuery)
                searchResults = results
                Log.d(TAG, "Search completed. Found ${results.size} results.")
                if (results.isEmpty()) {
                    // Optional: Set a specific message instead of just empty list
                    // error = "未找到匹配的用户" // Or display this in the list area
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during profile search", e)
                error = "搜索时出错: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // Adjust height based on content
            shape = MaterialTheme.shapes.medium, // Apply standard dialog shape
            tonalElevation = AlertDialogDefaults.TonalElevation // Apply standard elevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("搜索用户", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("输入用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null // Indicate error state on text field
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Area for Loading / Error / Results
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp) // Set min/max height for the list area
                ) {
                    when {
                        isSearching -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        error != null -> {
                            Text(
                                text = error ?: "未知错误",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center).padding(16.dp)
                            )
                        }
                        searchResults.isNotEmpty() -> {
                            LazyColumn {
                                items(searchResults, key = { it.userId }) { user -> // Use user.id as key
                                    if (user.userId != currentUserId) { // Double check not showing self
                                        SearchResultItem(
                                            user = user,
                                            contactRepository = contactRepository,
                                            currentUserId = currentUserId, // Pass non-null ID if available
                                            coroutineScope = coroutineScope
                                            // Add callbacks if needed to update overall state
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }
                        searchQuery.isNotBlank() && searchQuery.length >= 2 -> {
                            // Show only if a search was attempted (query is valid)
                            Text(
                                "未找到匹配的用户",
                                modifier = Modifier.align(Alignment.Center).padding(16.dp)
                            )
                        }
                        else -> {
                            // Initial state or query too short
                            Text(
                                if (searchQuery.isBlank()) "请输入用户名进行搜索" else "请输入至少2个字符",
                                modifier = Modifier.align(Alignment.Center).padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}


@Composable
private fun SearchResultItem(
    user: UserProfile,
    contactRepository: ContactRepository,
    currentUserId: String?,
    coroutineScope: CoroutineScope // Receive scope from parent
) {
    var contactStatus by remember { mutableStateOf<ContactStatus?>(null) }
    var isLoadingStatus by remember { mutableStateOf(true) } // Loading status initially
    var isSendingRequest by remember { mutableStateOf(false) } // Sending friend request status
    var requestError by remember { mutableStateOf<String?>(null) }

    // Fetch initial contact status when the item appears or user ID changes
    LaunchedEffect(user.userId, currentUserId) {
        if (currentUserId == null) {
            isLoadingStatus = false
            Log.w(TAG, "Cannot check status for item ${user.username}, currentUserId is null.")
            return@LaunchedEffect
        }
        isLoadingStatus = true
        requestError = null // Clear previous error
        Log.d(TAG, "Checking contact status for user: ${user.username} (${user.userId})")
        try {
            contactStatus = contactRepository.getContactStatus(currentUserId, user.userId)
            Log.d(TAG, "Status for ${user.username}: $contactStatus")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contact status for ${user.username}", e)
            requestError = "无法检查状态" // Set specific error for this item
        } finally {
            isLoadingStatus = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(user.username ?: "N/A", modifier = Modifier.weight(1f).padding(end = 8.dp))

        // Action Button Logic
        when {
            isLoadingStatus -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Small indicator
            }
            isSendingRequest -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Show spinner while sending
            }
            requestError != null -> {
                Text(requestError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
            contactStatus == ContactStatus.ACCEPTED -> {
                Text("已是好友", style = MaterialTheme.typography.bodyMedium)
            }
            contactStatus == ContactStatus.PENDING -> {
                // Check who sent the request (Optional, needs more info from getContactStatus or another query)
                // For simplicity, just show Pending
                Text("请求待处理", style = MaterialTheme.typography.bodyMedium)
            }
            contactStatus == ContactStatus.REJECTED -> {
                Text("已忽略", style = MaterialTheme.typography.bodyMedium)
            }
            else -> { // null or any other status means we can add
                Button(
                    onClick = {
                        if (currentUserId != null && !isSendingRequest) {
                            isSendingRequest = true
                            requestError = null // Clear previous error
                            coroutineScope.launch {
                                Log.d(TAG, "Sending friend request to: ${user.username} (${user.userId}) from $currentUserId")
                                try {
                                    val success = contactRepository.addContact(currentUserId, user.userId)
                                    if (success) {
                                        Log.i(TAG, "Friend request sent successfully to ${user.username}.")
                                        contactStatus = ContactStatus.PENDING // Optimistically update status
                                    } else {
                                        Log.w(TAG, "Failed to send friend request to ${user.username} (Repo returned false).")
                                        requestError = "发送失败" // Or get specific error from repo if possible
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Exception sending friend request to ${user.username}", e)
                                    requestError = "发送出错"
                                } finally {
                                    isSendingRequest = false
                                }
                            }
                        } else if (currentUserId == null) {
                            Log.e(TAG, "Cannot send request, currentUserId is null.")
                            requestError = "错误：未登录"
                        }
                    },
                    enabled = !isSendingRequest && currentUserId != null // Disable if sending or not logged in
                ) {
                    Text("添加好友")
                }
            }
        }
    }
}

/*
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

 */