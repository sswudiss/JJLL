package com.example.jjll.ui.screens.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.dialogs.SearchUserDialog
import com.example.jjll.ui.navigation.AppDestinations

/*
* 完善 ContactsScreen.kt 的 UI：
確保 UI 中有兩個獨立的部分（例如，兩個 LazyColumn 或一個 LazyColumn 包含不同的 item 類型）
來分別顯示來自 viewModel.friendRequests 和 viewModel.contacts 的數據。
好友請求列表項需要顯示請求者信息和 "接受" / "拒絕" 按鈕，按鈕的 onClick 應調用 viewModel.acceptFriendRequest(request.userId)
或 viewModel.rejectFriendRequest(request.userId)。
已接受聯繫人列表項 需要顯示聯繫人信息（名片），並且其頂層可點擊修飾符 (Modifier.clickable) 需要調用 navController.navigate(...)
來導航到 ChatDetailScreen，並傳遞正確的 contact.userId 和 contact.username。
*
代碼說明和要點：
狀態收集: 使用 collectAsStateWithLifecycle() 收集 ViewModel 的 StateFlow，這是 Jetpack Compose 中推薦的方式，能感知生命週期。
Scaffold 結構: 使用 Scaffold 提供標準的屏幕佈局，包含 TopAppBar。
TopAppBar: 顯示標題 "通讯录"，並在 actions 中添加了一個 IconButton，圖標是 Icons.Filled.Search，點擊時調用 viewModel.showSearchDialog()。
條件渲染: Box 內部使用 when 語句根據 isLoading 和 error 的狀態顯示不同的內容（加載指示器、錯誤信息或實際列表）。
SearchUserDialog: 當 isSearching 為 true 時，顯示搜索對話框。
注意，需要將 ContactRepository 和 AuthRepository 實例傳遞給它。為了做到這一點，
我在 ContactsViewModel 中添加了兩個簡單的 getter 方法 (getContactRepository, getAuthRepository)，
您需要在您的 ContactsViewModel.kt 中添加這兩個方法。
LazyColumn: 用於高效顯示可能很長的請求列表和聯繫人列表。
分區顯示: 在 LazyColumn 中，使用 item 顯示標題 ("好友请求", "我的联系人")，並使用 items 分別遍歷 friendRequests 和 contacts 列表。
添加了條件判斷 (isNotEmpty())，只在列表非空時顯示標題和內容。也添加了空狀態的提示文本。
FriendRequestItem: 單獨的 Composable 用於顯示單個好友請求，包含用戶名和 "接受"/"拒絕" 按鈕，按鈕的 onClick 回調連接到 ViewModel 的方法。
ContactItem: 單獨的 Composable 用於顯示單個聯繫人。
關鍵點: 整個 Row 使用 Modifier.clickable(onClick = onClick) 使其可點擊。
導航邏輯: 點擊時觸發的 onClick lambda 調用 navController.navigate()，
並使用 Screen.ChatDetail.route + "/${contact.userId}/${contact.username}" 構建正確的導航路徑，
將被點擊聯繫人的 userId 和 username 傳遞給 ChatDetailScreen。
key: 在 items 中使用 key = { it.userId } 可以幫助 Compose 更高效地處理列表項的重組、添加和刪除。
Divider: 在列表項之間添加了分隔線，改善視覺效果。
Padding 和 Spacing: 使用 Modifier.padding 和 Spacer 來調整佈局和間距。
* */

@OptIn(ExperimentalMaterial3Api::class) // Scaffold 和 TopAppBar 需要
@Composable
fun ContactsScreen(
    navController: NavController,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    // 條件顯示搜索對話框
    if (isSearching) {
        SearchUserDialog(
            onDismiss = viewModel::dismissSearchDialog,
            contactRepository = viewModel.getContactRepository(), // 傳遞 Repository 實例
            authRepository = viewModel.getAuthRepository(),
            profileRepository = TODO()       // 傳遞 Repository 實例
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通讯录") },
                actions = {
                    // 添加搜索按鈕
                    IconButton(onClick = viewModel::showSearchDialog) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Users"
                        )
                    }
                }
                // 可以根據需要添加 navigationIcon 等
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 应用 Scaffold 提供的 padding
        ) {
            when {
                isLoading -> {
                    // 显示加载指示器
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    // 显示错误信息
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    // 显示主要内容（请求和联系人列表）
                    ContactsContent(
                        navController = navController,
                        friendRequests = friendRequests,
                        contacts = contacts,
                        onAcceptRequest = { userId -> viewModel.acceptFriendRequest(userId) },
                        onRejectRequest = { userId -> viewModel.rejectFriendRequest(userId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsContent(
    navController: NavController,
    friendRequests: List<UserProfile>,
    contacts: List<UserProfile>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp) // 給列表內容添加內邊距
    ) {
        // 好友請求部分
        if (friendRequests.isNotEmpty()) {
            item {
                Text(
                    "好友请求",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(friendRequests, key = { it.userId }) { request -> // 使用 userId 作為 key
                FriendRequestItem(
                    request = request,
                    onAccept = { onAcceptRequest(request.userId) },
                    onReject = { onRejectRequest(request.userId) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp)) // 添加分隔線
            }
        } else {
            item {
                // 可以選擇性顯示 "無待處理請求"
                // Text("暂无好友请求", modifier = Modifier.padding(vertical = 8.dp))
            }
        }


        // 我的聯繫人部分
        if (contacts.isNotEmpty()) {
            item {
                Text(
                    "我的联系人",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp) // 與上一部分的間距
                )
            }
            items(contacts, key = { it.userId }) { contact -> // 使用 userId 作為 key
                ContactItem(
                    contact = contact,
                    onClick = {
                        // --- 關鍵：點擊聯繫人導航到聊天詳情頁 ---
                        navController.navigate(AppDestinations.ChatDetail.route + "/${contact.userId}/${contact.username}")
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // 添加分隔線
            }
        } else {
            item {
                // 可以選擇性顯示 "通訊錄為空"
                if (friendRequests.isEmpty()) { // 只有在也沒有請求時才顯示這個
                    Text("通讯录为空，快去搜索添加好友吧！", modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

// 好友請求列表項
@Composable
fun FriendRequestItem(
    request: UserProfile,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // 讓按鈕靠右
    ) {
        // 顯示請求者信息 (例如：用戶名)
        Text(request.username, modifier = Modifier.weight(1f).padding(end = 8.dp)) // 佔用多餘空間

        // 接受和拒絕按鈕
        Row {
            Button(onClick = onAccept, modifier = Modifier.padding(end = 8.dp)) {
                Text("接受")
            }
            OutlinedButton(onClick = onReject) { // 使用 OutlinedButton 區分
                Text("拒绝")
            }
        }
    }
}

// 聯繫人列表項 (名片)
@Composable
fun ContactItem(
    contact: UserProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // --- 關鍵：使整行可點擊 ---
            .padding(vertical = 12.dp), // 增加垂直內邊距使點擊區域更大
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 可以添加頭像 Icon(Icons.Default.Person, contentDescription = "Contact Avatar")
        Spacer(modifier = Modifier.width(16.dp)) // 頭像和名字的間距
        Text(contact.username, fontSize = 16.sp)
        // 可以添加其他信息，如在線狀態等
    }
}

// --- Helper function in ViewModel to pass repository ---
// 在 ContactsViewModel.kt 中添加這兩個方法，以便在 Screen 中獲取 Repository 實例傳遞給 Dialog
/*
fun getContactRepository(): ContactRepository = contactRepository
fun getAuthRepository(): AuthRepository = authRepository
*/