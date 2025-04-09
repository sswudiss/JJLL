package com.example.jjll.ui.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.ui.auth.AuthRepository
import com.example.jjll.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.NavigationEvent

//這個 ViewModel 將負責管理 "我的" 頁面的狀態和業務邏輯。


private const val TAG = "ProfileViewModel"

// 定義 Profile 頁面的 UI 狀態
data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val error: String? = null,
    val showLogoutConfirmDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ProfileUiState(isLoading = true)) // 初始狀態為加載中
    val uiState: State<ProfileUiState> = _uiState // UI 層觀察這個 State

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        fetchCurrentUserProfile() // ViewModel 初始化時自動加載用戶資料
    }

    fun fetchCurrentUserProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null) // 開始加載，清除舊錯誤
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.e(TAG, "Cannot fetch profile, current user ID is null.")
                _uiState.value = _uiState.value.copy(isLoading = false, error = "無法獲取用戶信息，請重新登錄")
                // 可能需要觸發登出導航
                // _navigationEvent.emit(NavigationEvent.NavigateToLogin)
                return@launch
            }

            Log.d(TAG, "Fetching profile for current user ID: $currentUserId")
            when (val result = profileRepository.getUserProfile(currentUserId)) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = result.data,
                        error = null
                    )
                    Log.i(TAG, "Profile fetched successfully: ${result.data.username}")
                }
                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = null, // 可以選擇不清空舊數據，看 UI 設計
                        error = result.message
                    )
                    Log.e(TAG, "Error fetching profile: ${result.message}", result.exception)
                }
                is RepoResult.Loading -> {
                    // RepoResult 也定義了 Loading，如果 Repository 內部有複雜加載狀態可以用到
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun requestLogout() {
        // 顯示登出確認對話框
        _uiState.value = _uiState.value.copy(showLogoutConfirmDialog = true)
    }

    fun confirmLogout() {
        _uiState.value = _uiState.value.copy(showLogoutConfirmDialog = false) // 關閉對話框
        viewModelScope.launch {
            Log.d(TAG, "User confirmed logout.")
            authRepository.signOut() // 調用 Repository 執行登出
            _navigationEvent.emit(NavigationEvent.NavigateToLogin) // 發送導航事件
        }
    }

    fun cancelLogout() {
        // 僅關閉對話框
        _uiState.value = _uiState.value.copy(showLogoutConfirmDialog = false)
    }

    // --- 後續功能的方法 ---
    fun updateDisplayName(newName: String) {
        val userId = _uiState.value.userProfile?.user_id ?: return // 需要用戶 ID
        if (newName.isBlank() || newName == _uiState.value.userProfile?.display_name) return // 簡單驗證

        viewModelScope.launch {
            // 可以添加 Loading 狀態
            when(profileRepository.updateDisplayName(userId, newName)) {
                is RepoResult.Success -> {
                    // 更新成功，刷新 Profile
                    fetchCurrentUserProfile() // 重新獲取最新數據
                    Log.i(TAG,"Display name update successful.")
                }
                is RepoResult.Error -> {
                    // 處理錯誤，例如顯示 SnackBar
                    Log.e(TAG,"Display name update failed: ${ (profileRepository.updateDisplayName(userId, newName) as RepoResult.Error).message}")
                    _uiState.value = _uiState.value.copy(error = "更新名稱失敗") // 簡單錯誤提示
                }
                else -> {}
            }
        }
    }
}