package com.example.jjll.ui.auth

//這個 ViewModel 將處理註冊和登錄的邏輯

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.JJLLAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// 定義內部郵箱後綴，應與 AuthRepository 中的一致
private const val INTERNAL_EMAIL_SUFFIX = "@jjll.org"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- 通用狀態 Flow ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- 一次性事件 Flow ---
    // 使用 SharedFlow 發送不需要粘性行為的事件
    private val _loginSuccessEvent = MutableSharedFlow<Unit>(replay = 0) // replay=0 確保非粘性
    val loginSuccessEvent = _loginSuccessEvent.asSharedFlow()

    private val _registrationSuccessEvent = MutableSharedFlow<Unit>(replay = 0)
    val registrationSuccessEvent = _registrationSuccessEvent.asSharedFlow()

    // --- 登錄邏輯 ---
    /**
     * 處理用戶登錄。
     * @param username 用戶輸入的用戶名
     * @param password 用戶輸入的密碼
     */
    fun login(username: String, password: String) {
        // 基本的客戶端驗證
        if (username.isBlank() || password.isBlank()) {
            _error.value = "用戶名和密碼不能為空。"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("AuthViewModel", "準備調用 AuthRepository 登錄，用戶名: $username")
                // 調用 Repository 的登錄方法 (內部會轉換郵箱)
                authRepository.loginWithUsername(username, password)
                Log.i("AuthViewModel", "登錄成功 for username: $username")
                _loginSuccessEvent.emit(Unit) // 發送登錄成功事件

            } catch (e: JJLLAuthException) {
                Log.e("AuthViewModel", "AuthRepository 登錄失敗 for $username: ${e.message}")
                _error.value = e.message // 直接顯示 Repository 處理過的錯誤信息
            } catch (e: Exception) {
                Log.e("AuthViewModel", "登錄時發生未知錯誤 for $username", e)
                _error.value = "登錄時發生未知錯誤: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false // 結束加載狀態
            }
        }
    }

    // --- 註冊邏輯 ---
    /**
     * 處理用戶註冊。
     * @param username 用戶選擇的用戶名
     * @param password 用戶設置的密碼
     */
    fun registerUser(username: String, password: String) {
        // 基本的客戶端驗證 (雖然 Screen 也做了，這裡可以再加一層)
        if (username.isBlank() || password.isBlank()) {
            _error.value = "用戶名和密碼不能為空。"
            return
        }
        // 可以添加更複雜的用戶名/密碼規則驗證

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // 清除之前的錯誤
            try {
                Log.d("AuthViewModel", "準備調用 AuthRepository 註冊，用戶名: $username")
                // 調用 Repository 的註冊方法 (內部會轉換郵箱)
                authRepository.registerWithUsername(username, password)
                Log.i("AuthViewModel", "註冊請求成功 for username: $username (可能需要驗證)")
                _registrationSuccessEvent.emit(Unit) // 發送註冊成功事件

            } catch (e: JJLLAuthException) {
                Log.e("AuthViewModel", "AuthRepository 註冊失敗 for $username: ${e.message}")
                _error.value = e.message // 顯示 Repository 處理過的錯誤信息
            } catch (e: Exception) {
                Log.e("AuthViewModel", "註冊時發生未知錯誤 for $username", e)
                _error.value = "註冊時發生未知錯誤: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false // 結束加載狀態
            }
        }
    }

    /**
     * (輔助函數) 清除錯誤信息。
     * 可以在用戶開始輸入時調用，以清除之前的錯誤提示。
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 同步檢查用戶在應用啟動時是否已登錄。
     * 注意：這只反映初始狀態，後續狀態變化應通過觀察 sessionStatus Flow。
     */
    fun isUserLoggedInInitially(): Boolean {
        // 直接調用 Repository 的同步方法
        return authRepository.getCurrentUserId() != null
    }
}


// --- 登出邏輯 (如果需要從 AuthViewModel 觸發) ---
// 雖然目前是由 ProfileViewModel 處理，但如果設計不同也可以放在這裡
/*
fun logout() {
    viewModelScope.launch {
         _isLoading.value = true // 顯示登出狀態
         try {
             authRepository.logout()
             // 需要一種方式通知 UI 登出完成 (例如 SharedFlow)
         } catch (e: Exception) {
             _error.value = "登出失敗: ${e.message}"
         } finally {
             _isLoading.value = false
         }
     }
}
*/
