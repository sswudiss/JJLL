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
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import kotlin.collections.map

private const val TAG = "ContactsViewModel"
private const val SEARCH_DEBOUNCE_MS = 300L

// 更新 UI State 以包含 pending requests
data class ContactsUiState(
    // 已接受聯繫人狀態
    val contacts: List<UserProfile> = emptyList(),
    val isLoadingContacts: Boolean = false,
    val contactsError: String? = null,

    // 待處理請求狀態
    val pendingRequests: List<PendingRequest> = emptyList(),
    val isLoadingPending: Boolean = false,
    val pendingError: String? = null,

    // 用戶搜索狀態
    val searchQuery: String = "",
    val searchResults: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,

    // 操作結果提示 (例如 Snackbar)
    val actionResult: String? = null // 合併 addContactResult
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository // 保持注入 AuthRepository 以獲取 userId
) : ViewModel() {

    private val _uiState = mutableStateOf(ContactsUiState())
    val uiState: State<ContactsUiState> = _uiState

    private var searchJob: Job? = null

    init {
        // 初始化時同時加載兩種列表
        loadAcceptedContacts()
        loadPendingRequests()
    }

    // --- 加載已接受聯繫人 ---
    fun loadAcceptedContacts() {
        _uiState.value =
            _uiState.value.copy(isLoadingContacts = true, contactsError = null, actionResult = null)
        viewModelScope.launch {
            when (val result = contactRepository.getAcceptedContacts()) {
                //RepoResult 的返回： 確認方法最終返回的是 RepoResult.Success(profiles) 並且 profiles 列表不為空
                is RepoResult.Success -> {
                    _uiState.value =
                        _uiState.value.copy(isLoadingContacts = false, contacts = result.data) // <-- 確保 result.data 被正確賦值給 uiState.contacts
                    Log.i(TAG, "Accepted contacts loaded: ${result.data.size}")  // <-- 確認這裡打印的 size > 0
                }

                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingContacts = false,
                        contactsError = result.message
                    )
                    Log.e(TAG, "Error loading contacts: ${result.message}", result.exception)
                }

                else -> _uiState.value =
                    _uiState.value.copy(isLoadingContacts = false) // 確保結束 Loading
            }
        }
    }

    // --- 加載待處理請求 ---
    fun loadPendingRequests() {
        _uiState.value =
            _uiState.value.copy(isLoadingPending = true, pendingError = null, actionResult = null)
        viewModelScope.launch {
            when (val result = contactRepository.getPendingRequests()) {
                is RepoResult.Success -> {
                    _uiState.value =
                        _uiState.value.copy(isLoadingPending = false, pendingRequests = result.data)
                    Log.i(TAG, "Pending requests loaded: ${result.data.size}")
                }

                is RepoResult.Error -> {
                    _uiState.value =
                        _uiState.value.copy(isLoadingPending = false, pendingError = result.message)
                    Log.e(
                        TAG,
                        "Error loading pending requests: ${result.message}",
                        result.exception
                    )
                }

                else -> _uiState.value = _uiState.value.copy(isLoadingPending = false)
            }
        }
    }

    // --- 搜索邏輯 (保持不變，但過濾時可能需要考慮 pending) ---
    fun onSearchQueryChange(query: String) {
        _uiState.value =
            _uiState.value.copy(searchQuery = query, searchError = null, actionResult = null)
        searchJob?.cancel()
        if (query.isBlank() || query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        _uiState.value = _uiState.value.copy(isSearching = true)
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            Log.d(TAG, "Debounced search triggered for query: $query")
            when (val result = profileRepository.searchUsersByUsername(query)) {

                else -> {
                    val currentContactsIds = _uiState.value.contacts.map { it.user_id }.toSet()
                    // 考慮：是否也要排除已發送請求但對方未處理的人？
                    // val pendingSentIds = // ... 需要額外查詢 ...
                    val currentUserId = authRepository.getCurrentUserId()
                    if (currentUserId == null) {
                        Log.w(TAG, "Current user ID null in search filter"); }
                    // 顯式轉換 result 為 Success 類型
                    val successResult = result as RepoResult.Success<List<UserProfile>>
                    val filteredResults = successResult.data.filter { // <-- 現在可以訪問 data
                        (currentUserId == null || it.user_id != currentUserId) && !currentContactsIds.contains(
                            it.user_id
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResults = filteredResults
                    )
                    Log.i(TAG, "Search results loaded for '$query': ${filteredResults.size}")
                }
            }
        }
    }

    // --- 發送聯繫人請求 (保持不變) ---
    fun sendContactRequest(contactUserId: String) {
        _uiState.value = _uiState.value.copy(actionResult = null)
        viewModelScope.launch {
            Log.d(TAG, "Sending contact request to $contactUserId")
            when (val result = contactRepository.addContact(contactUserId)) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionResult = "好友請求已發送")
                }

                is RepoResult.Error -> {
                    _uiState.value =
                        _uiState.value.copy(actionResult = "發送失敗: ${result.message}")
                }

                else -> {}
            }
            // 清除搜索結果中已發送的用戶 (可選)
            _uiState.value =
                _uiState.value.copy(searchResults = _uiState.value.searchResults.filterNot { it.user_id == contactUserId })
        }
    }

    // --- 新增：接受聯繫人請求 ---
    fun acceptContactRequest(contactId: Long) {
        _uiState.value = _uiState.value.copy(actionResult = null) // 清除舊消息
        viewModelScope.launch {
            Log.d(TAG, "Accepting contact request with id: $contactId")
            when (val result = contactRepository.updateContactStatus(contactId, "accepted")) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionResult = "已添加好友")
                    Log.i(TAG, "Contact request accepted successfully (id: $contactId)")
                    // 重要：刷新兩個列表
                    loadAcceptedContacts()
                    loadPendingRequests()
                }

                is RepoResult.Error -> {
                    _uiState.value =
                        _uiState.value.copy(actionResult = "接受請求失敗: ${result.message}")
                    Log.e(
                        TAG,
                        "Error accepting contact request (id: $contactId): ${result.message}"
                    )
                }

                else -> {}
            }
        }
    }

    // --- 新增：拒絕/忽略聯繫人請求 ---
    fun rejectContactRequest(contactId: Long) {
        _uiState.value = _uiState.value.copy(actionResult = null)
        viewModelScope.launch {
            // 我們選擇更新狀態為 'blocked' 來表示拒絕/忽略
            // RLS 策略允許 pending -> blocked
            Log.d(TAG, "Rejecting contact request with id: $contactId by setting status to blocked")
            when (val result = contactRepository.updateContactStatus(contactId, "blocked")) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionResult = "已忽略好友請求")
                    Log.i(TAG, "Contact request rejected successfully (id: $contactId)")
                    // 只刷新待處理列表
                    loadPendingRequests()
                }

                is RepoResult.Error -> {
                    _uiState.value =
                        _uiState.value.copy(actionResult = "忽略請求失敗: ${result.message}")
                    Log.e(
                        TAG,
                        "Error rejecting contact request (id: $contactId): ${result.message}"
                    )
                }

                else -> {}
            }
        }
    }

    // --- 清除搜索狀態 (保持不變) ---
    fun clearSearchState() { /* ... */
    }

    // --- 清除操作結果消息 (用於 Snackbar) ---
    fun clearActionResult() {
        _uiState.value = _uiState.value.copy(actionResult = null)
    }
}