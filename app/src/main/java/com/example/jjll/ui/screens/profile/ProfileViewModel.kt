package com.example.jjll.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.JJLLAuthException
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import com.example.jjll.ui.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


//這個 ViewModel 將負責管理 "我的" 頁面的狀態和業務邏輯。

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- 狀態 Flow ---
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- 登出相關狀態 (可選) ---
    // 可以添加一個狀態來通知 UI 登出操作是否完成或失敗
    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    init {
        // ViewModel 初始化時自動加載用戶資料
        loadUserProfile()
    }

    /**
     * 從 Repository 加載當前用戶的 Profile。
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("ProfileViewModel", "正在加載當前用戶資料...")
                // 調用 ProfileRepository 的方法獲取當前用戶信息
                _userProfile.value = profileRepository.getCurrentUserProfile()
                if (_userProfile.value == null) {
                    // 如果返回 null 但沒有拋出 AuthException，可能是其他錯誤或 Profile 不存在
                    Log.w("ProfileViewModel", "無法獲取用戶資料，可能未找到對應 Profile。")
                    _error.value = "無法加載用戶資料。" // 可以提供更具體的錯誤
                } else {
                    Log.d("ProfileViewModel", "用戶資料加載成功: ${_userProfile.value?.username}")
                }
            } catch (e: JJLLAuthException) {
                Log.e("ProfileViewModel", "加載用戶資料時認證錯誤", e)
                _error.value = "認證失敗，請重新登錄。"
                _userProfile.value = null // 清空舊數據
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "加載用戶資料時發生未知錯誤", e)
                _error.value = "加載用戶資料失敗: ${e.message}"
                _userProfile.value = null // 清空舊數據
            } finally {
                _isLoading.value = false // 結束加載狀態
            }
        }
    }

    /**
     * 處理用戶登出操作。
     * 調用 AuthRepository 的登出方法。
     * **注意：** 實際的導航操作（跳轉到登錄頁）應該由 UI 層的回調觸發。
     */
    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading // 開始登出
            try {
                Log.d("ProfileViewModel", "正在執行登出操作...")
                authRepository.logout() // 調用 Repository 的登出方法
                Log.i("ProfileViewModel", "登出成功。")
                _logoutState.value = LogoutState.Success
                // 登出成功後，清空本地用戶信息是個好習慣
                _userProfile.value = null
                // 導航應由 UI 層的 onLogout 回調處理，ViewModel 不直接控制導航
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "登出時發生錯誤", e)
                _logoutState.value = LogoutState.Error("登出失敗: ${e.message}")
            } finally {
                // 可以選擇在短暫延遲後重置 LogoutState 回 Idle
                // kotlinx.coroutines.delay(2000)
                // _logoutState.value = LogoutState.Idle
            }
        }
    }
}

// --- (可選) 定義登出操作的狀態 ---
sealed class LogoutState {
    object Idle : LogoutState() // 初始狀態
    object Loading : LogoutState() // 正在登出
    object Success : LogoutState() // 登出成功
    data class Error(val message: String?) : LogoutState() // 登出失敗
}