package com.example.jjll.ui.screens.chat


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.jjll.data.Message
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class) // 需要用於 Scaffold, TopAppBar, TextField 等
@Composable
fun ChatDetailScreen(
    navController: NavController,
    viewModel: ChatDetailViewModel = hiltViewModel() // 使用 Hilt 注入 ViewModel
) {
    // --- 從 ViewModel 收集狀態 ---
    val contactProfile by viewModel.contactProfile.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle() // 假設 ViewModel 提供 StateFlow<List<Message>>
    val messageInput by viewModel.messageInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // --- 獲取當前用戶 ID (用於區分消息) ---
    // 這個 ID 必須可靠獲取，通常由 ViewModel 提供
    val currentUserId = viewModel.getCurrentUserId() // 假設 ViewModel 有此方法

    // --- UI 狀態和控制器 ---
    val listState = rememberLazyListState() // 用於控制列表滾動
    val coroutineScope = rememberCoroutineScope() // 用於啟動協程（例如滾動）
    val keyboardController = LocalSoftwareKeyboardController.current // 用於控制軟鍵盤

    // --- 效果處理 (Effects) ---
    // 當消息列表大小變化時，自動滾動到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1) // 滾動到最後一項
            }
        }
    }

    // --- UI 結構 ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // 顯示聊天對象的用戶名，處理 null 情況
                        text = contactProfile?.username ?: "加載中...",
                        maxLines = 1, // 最多一行
                        overflow = TextOverflow.Ellipsis // 超出部分顯示省略號
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) { // 返回按鈕
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
                // 可以在此處添加更多頂部欄操作，如視頻通話、查看資料等
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 應用 Scaffold 的內邊距
            // 可以考慮添加 .imePadding() 來更好地處理鍵盤遮擋，
            // 但通常 Column + weight + adjustResize 已能較好工作
        ) {
            // --- 消息列表、加載和錯誤顯示區域 ---
            Box(
                modifier = Modifier
                    .weight(1f) // 佔據頂部和輸入框之間的剩餘空間
                    .fillMaxWidth()
            ) {
                // 初始加載狀態 (列表為空時顯示)
                if (isLoading && messages.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 錯誤信息顯示 (可以考慮使用 Snackbar)
                error?.let { errorMsg ->
                    Text(
                        text = "錯誤: $errorMsg",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter) // 在頂部顯示錯誤以便看到
                    )
                }

                // --- 消息列表 ---
                LazyColumn(
                    state = listState, // 關聯 LazyListState
                    modifier = Modifier
                        .fillMaxSize() // 填滿 Box
                        .padding(horizontal = 8.dp), // 左右內邊距
                    contentPadding = PaddingValues(vertical = 8.dp) // 上下內邊距
                    // reverseLayout = true // 如果想讓新消息出現在底部且列表從下往上滾動
                ) {
                    items(messages, key = { it.id }) { message -> // 使用 message.id 作為 key
                        // 必須知道當前用戶 ID 才能正確顯示消息對齊
                        if (currentUserId != null) {
                            MessageItem(
                                message = message,
                                isSentByCurrentUser = message.senderId == currentUserId // 判斷是否為當前用戶發送
                            )
                        } else {
                            // 處理無法獲取當前用戶 ID 的情況 (例如顯示加載中或錯誤)
                            Log.w("ChatDetailScreen", "無法確定消息 ${message.id} 的發送者，因為 currentUserId 為空")
                            // 可以選擇顯示一個通用樣式的消息，或是不顯示
                        }
                    }
                }
            } // Box 結束

            // --- 底部輸入區域 ---
            ChatInput(
                value = messageInput,
                onValueChange = viewModel::onMessageInputChange, // 將輸入變化委託給 ViewModel
                onSendClick = {
                    // 在點擊發送時，確保輸入不為空 (雙重檢查)
                    if (messageInput.isNotBlank()) {
                        viewModel.sendMessage() // 調用 ViewModel 的發送方法
                        // 可選：發送後清空鍵盤
                        // keyboardController?.hide()
                    }
                },
                // 可以根據 isLoading 狀態禁用輸入框和按鈕
                // enabled = !isLoading
                enabled = true // 暫時保持啟用
            )
        } // Column 結束
    } // Scaffold 結束
}

/**
 * 用於顯示單條聊天消息氣泡的 Composable。
 */
@Composable
fun MessageItem(message: Message, isSentByCurrentUser: Boolean) {
    // 根據發送者確定對齊方式、背景色和氣泡形狀
    val alignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant // 接收方使用不同的背景色
    val textColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isSentByCurrentUser) { // 不同方向的尖角效果
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 消息之間的垂直間距
        contentAlignment = alignment // 控制氣泡在行內的對齊（左或右）
    ) {
        Box(
            modifier = Modifier
                // 限制氣泡最大寬度，避免過長消息佔滿整行
                .fillMaxWidth(0.8f) // 例如，最大佔用 80% 寬度
                // 確保內容在限制寬度內正確對齊
                .wrapContentWidth(if (isSentByCurrentUser) Alignment.End else Alignment.Start)
                .clip(bubbleShape) // 應用氣泡形狀
                .background(backgroundColor) // 設置背景色
                .padding(horizontal = 12.dp, vertical = 8.dp) // 氣泡內邊距
        ) {
            Column { // 使用 Column 以便未來在文本下方添加時間戳等信息
                Text(
                    text = message.content, // 顯示消息內容
                    color = textColor,      // 設置文本顏色
                    fontSize = 16.sp        // 設置字體大小
                )
                // 可選：在此處添加時間戳
                /*
                Text(
                    text = formatTimestamp(message.createdAt), // 需要格式化函數
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f), // 時間戳顏色稍暗
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
                */
            }
        }
    }
}

/**
 * 用於顯示聊天輸入框和發送按鈕的 Composable。
 */
@Composable
fun ChatInput(
    value: String,                     // 輸入框的當前值
    onValueChange: (String) -> Unit, // 輸入值變化時的回調
    onSendClick: () -> Unit,           // 點擊發送按鈕的回調
    enabled: Boolean = true            // 控制輸入區域是否可用
) {
    // 使用 Surface 可以在視覺上將輸入區域與消息列表分開，並可設置陰影
    Surface(
        tonalElevation = 4.dp, // 添加輕微的海拔陰影效果
        modifier = Modifier.fillMaxWidth() // 佔滿父佈局寬度
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp), // 輸入區域的內邊距
            verticalAlignment = Alignment.CenterVertically // 垂直居中對齊輸入框和按鈕
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f), // 讓輸入框佔據大部分空間
                placeholder = { Text("輸入消息...") }, // 提示文字
                shape = RoundedCornerShape(24.dp),   // 圓角邊框
                enabled = enabled,                 // 控制是否可編輯
                maxLines = 5,                      // 限制最大行數，防止無限增高
                // 可以添加鍵盤選項，例如在鍵盤上顯示發送按鈕
                // keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                // keyboardActions = KeyboardActions(onSend = { onSendClick() })
            )

            Spacer(modifier = Modifier.width(8.dp)) // 輸入框和按鈕之間的間距

            // 發送按鈕
            IconButton(
                onClick = onSendClick,
                // 只有在輸入框有內容且輸入區域啟用時才可點擊
                enabled = value.isNotBlank() && enabled,
                modifier = Modifier.size(48.dp) // 設置固定大小
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "發送消息",
                    // 按鈕啟用時使用主題色，禁用時使用灰色
                    tint = if (value.isNotBlank() && enabled) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

// --- 輔助函數 ---
// 需要您實現時間戳格式化邏輯
// fun formatTimestamp(timestamp: String?): String { ... }