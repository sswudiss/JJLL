package com.example.jjll.ui.screens.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.screens.contacts.ContactRepository

//這個 ViewModel 的職責是獲取用於顯示在聊天列表上的數據（初期是聯繫人）。

private const val TAG = "ChatListViewModel"

// ChatList 頁面的 UI 狀態
data class ChatListUiState(
    val chatPartners: List<UserProfile> = emptyList(), // 初期存放聯繫人作為聊天對象
    val isLoading: Boolean = false,
    val error: String? = null
    // 後期可以添加 lastMessage, unreadCount 等字段
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val contactRepository: ContactRepository // 注入 ContactRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ChatListUiState(isLoading = true))
    val uiState: State<ChatListUiState> = _uiState

    init {
        loadChatPartners() // 初始化時加載
    }

    fun loadChatPartners() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            // 複用 ContactRepository 的方法獲取已接受的聯繫人
            when (val result = contactRepository.getAcceptedContacts()) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        chatPartners = result.data // 將聯繫人列表賦值給 chatPartners
                    )
                    Log.i(TAG, "Chat partners (contacts) loaded: ${result.data.size}")
                }
                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                    Log.e(TAG, "Error loading chat partners: ${result.message}", result.exception)
                }
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}