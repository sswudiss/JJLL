package com.example.jjll.ui.screens.contacts

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay // 用於搜索防抖
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.JJLLAuthException
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import kotlin.collections.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/*
狀態管理: 使用 StateFlow 來管理好友請求列表 (_friendRequests)、聯繫人列表 (_contacts)、加載狀態 (_isLoading) 和錯誤信息 (_error)。
依賴注入: 正確注入了 ContactRepository 和 AuthRepository。
初始化: 在 init 塊中獲取當前用戶 ID，並調用 loadFriendRequests() 和 loadContacts() 來加載初始數據。
加載邏輯:
loadFriendRequests(): 調用 contactRepository.getFriendRequests(userId) 來獲取待處理的請求。
loadContacts(): 調用 contactRepository.getAcceptedContacts(userId) 來獲取已接受的聯繫人。
請求處理: 提供了 acceptFriendRequest(senderId) 和 rejectFriendRequest(senderId) 方法，它們調用 ContactRepository 中的相應方法來更新狀態，並在成功後重新加載兩個列表 (loadFriendRequests() 和 loadContacts()) 以刷新 UI。這一步非常關鍵且正確！
搜索對話框: 提供了控制搜索用戶對話框顯示/隱藏的方法。
* */

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository, // 注入 ContactRepository
    private val authRepository: AuthRepository    // 注入 AuthRepository
) : ViewModel() {

    // 使用 UserProfile 類型
    private val _friendRequests = MutableStateFlow<List<UserProfile>>(emptyList())
    val friendRequests: StateFlow<List<UserProfile>> = _friendRequests.asStateFlow()

    // 使用 UserProfile 類型
    private val _contacts = MutableStateFlow<List<UserProfile>>(emptyList())
    val contacts: StateFlow<List<UserProfile>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            try {
                // 獲取當前用戶 ID (假設 AuthRepository 返回 String?)
                currentUserId = authRepository.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "用戶未登錄。"
                    Log.e("ContactsViewModel", "Current User ID is null.")
                    return@launch
                }
                Log.d("ContactsViewModel", "Current User ID: $currentUserId")
                // 加載初始數據
                loadFriendRequests()
                loadContacts()
            } catch (e: JJLLAuthException) {
                Log.e("ContactsViewModel", "Auth error during init", e)
                _error.value = "認證錯誤: ${e.message}。請重新登錄。"
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error during initialization", e)
                _error.value = "初始化失敗: ${e.message}"
            }
        }
    }

    fun loadFriendRequests() {
        val userId = currentUserId ?: return // 確保用戶 ID 可用
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("ContactsViewModel", "Loading friend requests for user: $userId")
                // 調用 ContactRepository 的方法
                _friendRequests.value = contactRepository.getFriendRequests(userId)
                Log.d("ContactsViewModel", "Loaded ${_friendRequests.value.size} friend requests.")
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error loading friend requests", e)
                _error.value = "加載好友請求失敗: ${e.message}"
                _friendRequests.value = emptyList() // 出錯時清空列表
            } finally {
                // 考慮到 loadContacts() 也會設置 isLoading，這裡可能不需要立即設置為 false
                // 但如果兩個加載是獨立的 UI 反饋，則需要更精細的控制
            }
        }
    }

    fun loadContacts() {
        val userId = currentUserId ?: return // 確保用戶 ID 可用
        viewModelScope.launch {
            _isLoading.value = true // 假設開始加載整個屏幕
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("ContactsViewModel", "Loading accepted contacts for user: $userId")
                // 調用 ContactRepository 的方法
                _contacts.value = contactRepository.getAcceptedContacts(userId)
                Log.d("ContactsViewModel", "Loaded ${_contacts.value.size} contacts.")
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error loading contacts", e)
                _error.value = "加載聯繫人失敗: ${e.message}"
                _contacts.value = emptyList() // 出錯時清空列表
            } finally {
                _isLoading.value = false // 在兩個加載都嘗試完成後設置為 false
            }
        }
    }

    fun acceptFriendRequest(senderId: String) {
        val receiverId = currentUserId ?: return // 確保接收者 ID (當前用戶) 可用
        viewModelScope.launch {
            _isLoading.value = true // 指示正在處理
            _error.value = null
            try {
                Log.d("ContactsViewModel", "Accepting friend request from $senderId for $receiverId")
                // 調用 ContactRepository 的方法
                contactRepository.acceptFriendRequest(senderId, receiverId)
                Log.i("ContactsViewModel", "Friend request from $senderId accepted.")
                // 接受成功後，刷新好友請求列表和聯繫人列表
                loadFriendRequests()
                loadContacts() // 會在結束時將 isLoading 設為 false
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error accepting friend request", e)
                _error.value = "接受請求失敗: ${e.message}"
                _isLoading.value = false // 確保出錯時停止加載狀態
            }
        }
    }

    fun rejectFriendRequest(senderId: String) {
        val receiverId = currentUserId ?: return // 確保接收者 ID (當前用戶) 可用
        viewModelScope.launch {
            _isLoading.value = true // 指示正在處理
            _error.value = null
            try {
                Log.d("ContactsViewModel", "Rejecting friend request from $senderId for $receiverId")
                // 調用 ContactRepository 的方法
                contactRepository.rejectFriendRequest(senderId, receiverId)
                Log.i("ContactsViewModel", "Friend request from $senderId rejected.")
                // 拒絕/忽略成功後，只需要刷新好友請求列表
                loadFriendRequests()
                _isLoading.value = false // 手動設置為 false，因為只刷新了一個列表
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error rejecting friend request", e)
                _error.value = "拒絕請求失敗: ${e.message}"
                _isLoading.value = false // 確保出錯時停止加載狀態
            }
        }
    }

    fun showSearchDialog() {
        _isSearching.value = true
    }

    fun dismissSearchDialog() {
        _isSearching.value = false
        // 可選：關閉對話框後刷新列表，以防用戶剛剛發送了請求
        loadFriendRequests()
        loadContacts()
    }

    // --- 輔助函數：允許 Screen 獲取 Repository 實例傳遞給 Dialog ---
    fun getContactRepository(): ContactRepository = contactRepository
    fun getAuthRepository(): AuthRepository = authRepository
    // --- 輔助函數結束 ---

}