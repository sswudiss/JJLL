package com.example.jjll.ui.screens.chat

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.ChatListItem
import com.example.jjll.data.JJLLAuthException
import com.example.jjll.ui.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


//這個 ViewModel 的職責是獲取用於顯示在聊天列表上的數據（初期是聯繫人）。

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository, // 注入 ChatRepository (或 ChatListRepository)
    private val authRepository: AuthRepository     // 注入 AuthRepository 以獲取當前用戶ID
) : ViewModel() {

    // --- 狀態 Flow ---
    private val _chatList = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chatList: StateFlow<List<ChatListItem>> = _chatList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- 內部狀態 ---
    private var currentUserId: String? = null

    init {
        // 初始化時獲取當前用戶 ID 並加載數據
        viewModelScope.launch {
            try {
                currentUserId = authRepository.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "用戶未登錄，無法加載聊天列表。"
                    Log.e("ChatListViewModel", "初始化失敗: 無法獲取當前用戶ID")
                    return@launch
                }
                Log.d("ChatListViewModel", "當前用戶ID: $currentUserId，準備加載聊天列表")
                loadChatList() // 調用加載函數

            } catch (e: JJLLAuthException) {
                Log.e("ChatListViewModel", "Auth error during init", e)
                _error.value = "認證錯誤: ${e.message}。"
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Error during initialization", e)
                _error.value = "初始化失敗: ${e.message}"
            }
        }
    }

    /**
     * 從 Repository 加載聊天列表數據。
     */
    private fun loadChatList() {
        val userId = currentUserId ?: run {
            _error.value = "用戶 ID 無效，無法加載。"
            return // 如果 currentUserId 仍為 null，則不執行加載
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("ChatListViewModel", "正在為用戶 $userId 加載聊天列表...")

                // --- 核心：調用 Repository 方法獲取聊天列表 ---
                // ** 您需要確保 chatRepository 中存在 getChatListItems 方法 **
                // ** 並且該方法能正確返回 List<ChatListItem> **
                val fetchedList = chatRepository.getChatListItems(userId) // 調用 Repository

                // 可以根據需要對列表進行排序（例如按最後一條消息時間降序）
                // val sortedList = fetchedList.sortedByDescending { it.lastMessage?.timestamp }
                // _chatList.value = sortedList

                _chatList.value = fetchedList // 直接更新狀態
                Log.d("ChatListViewModel", "成功加載 ${fetchedList.size} 個聊天會話。")

            } catch (e: JJLLAuthException) {
                Log.e("ChatListViewModel", "加載聊天列表時認證錯誤", e)
                _error.value = "認證錯誤: ${e.message}。"
                _chatList.value = emptyList() // 清空列表
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "加載聊天列表時出錯", e)
                _error.value = "加載聊天列表失敗: ${e.message}"
                _chatList.value = emptyList() // 清空列表
            } finally {
                _isLoading.value = false // 無論成功或失敗，結束加載狀態
            }
        }
    }

    /**
     * 提供給 UI 的刷新操作（如果需要手動刷新功能）。
     */
    fun refreshChatList() {
        Log.d("ChatListViewModel", "收到刷新請求。")
        // 重新獲取用戶 ID 以防萬一（雖然在 init 中已獲取）
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                loadChatList()
            } else {
                _error.value = "用戶未登錄，無法刷新。"
            }
        }
    }
}