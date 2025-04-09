package com.example.jjll.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jjll.R
import com.example.jjll.data.Message
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.theme.JJLLTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: androidx.navigation.NavHostController, // 需要用於返回
    viewModel: ChatDetailViewModel = hiltViewModel()
    // conversationId 不再需要直接傳入，ViewModel 從 SavedStateHandle 獲取
) {
    val uiState by viewModel.uiState
    val currentUserId = viewModel.currentUserId // 從 ViewModel 獲取當前用戶 ID

    // 用於控制 LazyColumn 滾動
    val listState = rememberLazyListState()
    // 用於自動滾動到底部
    val coroutineScope = rememberCoroutineScope()

    // 當消息列表變化時，滾動到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            // 使用 animateScrollToItem 可能更平滑，但需要計算好 index
            // listState.animateScrollToItem(uiState.messages.size - 1)
            listState.scrollToItem(uiState.messages.size - 1) // 立即滾動
        }
    }

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                contactProfile = uiState.contactProfile,
                isLoading = uiState.isLoadingContact,
                onBackClick = { navController.popBackStack() }, // 返回上一頁
                onVideoCallClick = { /* TODO */ },
                onMenuClick = { /* TODO */ }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // 移除 Scaffold 默認 insets，讓 imePadding 生效
    ) { paddingValues -> // 獲取 Scaffold 計算的 Padding (主要來自 TopBar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 只應用頂部 padding，底部由 imePadding 和 navigationBarsPadding 處理
                .padding(top = paddingValues.calculateTopPadding())
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)) // 聊天背景色
        ) {
            // 消息列表區域
            MessageList(
                messages = uiState.messages,
                currentUserId = currentUserId,
                modifier = Modifier
                    .weight(1f) // 佔滿除輸入框外的所有空間
                    .imePadding() // 在鍵盤彈出時，自動添加 padding 推高列表
            )

            // 輸入區域
            MessageInputArea(
                messageInput = uiState.messageInput,
                onInputChange = viewModel::onMessageInputChange,
                onSendMessage = viewModel::sendMessage,
                modifier = Modifier
                    .navigationBarsPadding() // 在系統導航欄之上添加 padding
                    .imePadding() // 在鍵盤彈出時，也推高輸入框 (可選，看效果)
                    .fillMaxWidth()
            )
        }
    }
}

// --- 自定義頂部欄 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailTopBar(
    contactProfile: UserProfile?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 對方頭像 (可選)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(contactProfile?.avatar_url)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .fallback(R.drawable.ic_avatar_placeholder)
                        .build(),
                    contentDescription = "對方頭像",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 對方名稱
                Text(
                    text = if (isLoading) "加載中..." else contactProfile?.display_name
                        ?: contactProfile?.username ?: "未知用戶",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onVideoCallClick) {
                Icon(Icons.Filled.Videocam, contentDescription = "視頻通話") // 佔位
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多選項") // 佔位
            }
        }
        // colors = TopAppBarDefaults.topAppBarColors(...) // 自定義顏色
    )
}


// --- 消息列表 ---
@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String?,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState() // 傳入或記住 ListState
) {
    val focusManager = LocalFocusManager.current // 用於隱藏鍵盤

    LazyColumn(
        state = listState, // 使用狀態控制滾動
        modifier = modifier
            .fillMaxSize()
            // 點擊列表區域收起鍵盤
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // 消息間的垂直間距
    ) {
        itemsIndexed(
            messages,
            key = { index, message -> message.id }) { index, message -> // 使用帶索引的 items 和 key
            val isSentByMe = message.sender_id == currentUserId
            MessageBubble(message = message, isSentByMe = isSentByMe)
        }
    }
}

// --- 單個消息氣泡 ---
@Composable
private fun MessageBubble(message: Message, isSentByMe: Boolean) {
    // 使用 Box 控制對齊
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isSentByMe) 64.dp else 0.dp, // 自己發的靠右，左側留空
                end = if (isSentByMe) 0.dp else 64.dp    // 對方發的靠左，右側留空
            ),
        contentAlignment = if (isSentByMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isSentByMe) 16.dp else 0.dp, // 根據發送者調整圓角
                bottomEnd = if (isSentByMe) 0.dp else 16.dp
            ),
            color = if (isSentByMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, // 根據發送者設置不同背景色
            tonalElevation = 1.dp, // 添加一點點陰影
            modifier = Modifier.padding(vertical = 4.dp) // 氣泡自身的垂直內邊距
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp) // 氣泡內文字邊距
            )
            // TODO: 添加時間戳、已讀狀態等
        }
    }
}


// --- 底部輸入區域 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInputArea(
    messageInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface( // 給輸入區域加個背景和陰影
        modifier = modifier,
        tonalElevation = 3.dp // 比消息列表高一點的陰影
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp), // 輸入區域內邊距
            verticalAlignment = Alignment.Bottom // 讓輸入框和按鈕底部對齊
        ) {
            // 可以加一個 "+" 按鈕用於附件
            IconButton(
                onClick = { /* TODO: 附件菜單 */ },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = "附加文件")
            }
            Spacer(modifier = Modifier.width(8.dp))

            // 輸入框 (使用 BasicTextField 可能更靈活)
            OutlinedTextField( // 或者 TextField
                value = messageInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f), // 佔滿中間空間
                placeholder = { Text("輸入消息...") },

                // 設置最大行數和自適應高度 (需要實驗)
                // maxLines = 5,
                shape = RoundedCornerShape(24.dp), // 圓角輸入框
                // 自定義顏色和內邊距   outlinedTextFieldColors
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
                //OutlinedTextField 的內這個錯誤 None of the following candidates is applicable 發生在調用 OutlinedTextField 時，
                //意味著你傳遞給函數的參數組合與 OutlinedTextField 的任何一個可用重載版本的簽名都不匹配。
                //錯誤信息列邊距通常由其內部實現和傳遞給 colors 的配置（或者其 modifier 的 padding）來控制，
                // 它沒有直接的 contentPadding 參數
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 發送按鈕 (根據輸入內容切換?)
            val isInputEmpty = messageInput.isBlank()
            Button( // 或者用 IconButton
                onClick = {
                    if (!isInputEmpty) onSendMessage() else { /* 處理空狀態點擊? */
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(48.dp), // 控制按鈕大小
                shape = CircleShape, // 圓形按鈕
                contentPadding = PaddingValues(0.dp), // 移除默認 padding
                enabled = !isInputEmpty // 輸入為空時禁用 (或改變圖標)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "發送消息"
                )
            }
        }
    }
}

// --- Previews ---
// Preview 可能需要創建 Mock ViewModel 或提供靜態數據
// ... (省略 ChatDetailScreen 預覽，因為它依賴 ViewModel)

@Preview(showBackground = true)
@Composable
fun ChatDetailTopBarPreview() {
    JJLLTheme {
        val profile = UserProfile("id", "contact_user", "聊天對象")
        ChatDetailTopBar(
            contactProfile = profile,
            isLoading = false,
            onBackClick = {},
            onVideoCallClick = {},
            onMenuClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubbleSentPreview() {
    JJLLTheme {
        val msg = Message(1L, "me", "other", "這是我發送的消息", "10:00")
        MessageBubble(message = msg, isSentByMe = true)
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubbleReceivedPreview() {
    JJLLTheme {
        val msg = Message(2L, "other", "me", "這是收到的消息，可能會長一點換行看看效果。", "10:01")
        MessageBubble(message = msg, isSentByMe = false)
    }
}


@Preview(showBackground = true)
@Composable
fun MessageInputAreaPreview() {
    JJLLTheme {
        var text by remember { mutableStateOf("輸入一些文字") }
        MessageInputArea(
            messageInput = text,
            onInputChange = { text = it },
            onSendMessage = {}
        )
    }
}