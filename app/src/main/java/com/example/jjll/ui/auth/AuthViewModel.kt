package com.example.jjll.ui.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

//這個 ViewModel 將處理註冊和登錄的邏輯

// 定義 Log Tag
private const val TAG = "AuthViewModel"

// 封裝註冊/登錄結果，可以包含成功信息或錯誤信息
sealed class AuthResult {
    object Success : AuthResult() // 代表成功
    data class Error(val message: String) : AuthResult() // 代表失敗及錯誤消息
    object Loading : AuthResult() // 代表正在處理中
    object Idle : AuthResult() // 代表初始或空閒狀態
}

// 定義固定域名
private const val EMAIL_DOMAIN = "@jjll.org" // 考慮移到更全局的配置中

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository // 注入 Repository (更好的實踐)
    // private val supabaseClient: SupabaseClient // 或者直接注入 Client (簡單直接，初期可行)
) : ViewModel() {

    // --- UI State ---
    var password by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("") // 僅註冊時使用
        private set
    var username by mutableStateOf("") // 註冊時使用
        private set
    var loginInput by mutableStateOf("") // <--- 新增: 用於登錄頁輸入 (代表用戶名)
    var passwordVisible by mutableStateOf(false) // 控制密碼可見性
        private set
    var confirmPasswordVisible by mutableStateOf(false) // 控制確認密碼可見性
        private set

    // --- Result State ---
    // 使用 MutableStateFlow 或 LiveData 通常更好，但這裡為了演示清晰，暫用 mutableStateOf
    // 對於一次性事件（如導航或 SnackBar），SharedFlow 更合適
    var authResultState by mutableStateOf<AuthResult>(AuthResult.Idle)
        private set

    // 用於觸發一次性事件，如導航
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // --- Event Handlers ---

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        if (authResultState is AuthResult.Error) authResultState = AuthResult.Idle
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
        if (authResultState is AuthResult.Error) authResultState = AuthResult.Idle
    }

    fun onUsernameChange(newUsername: String) { // 用於註冊
        username = newUsername.trim() // 去除前後空格
        if (authResultState is AuthResult.Error) authResultState = AuthResult.Idle
    }

    fun onLoginInputChange(input: String) { // <--- 新增: 用於處理登錄頁的用戶名輸入
        loginInput = input.trim()
        if (authResultState is AuthResult.Error) authResultState = AuthResult.Idle
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible
    }


    // --- Actions ---
    fun signUp() {
        authResultState = AuthResult.Loading // 開始處理，顯示加載狀態

        Log.d(TAG, "Attempting to sign up with username: '$username'") // 添加日誌記錄嘗試的用戶名

        // 基本驗證
        if (username.isBlank()) {
            Log.w(TAG, "Sign up validation failed: Username is blank.") // 添加警告日誌
            authResultState = AuthResult.Error("用戶名不能為空")
            return
        }

        // 用戶名格式驗證 (添加日誌)
        // 正則表達式: ^[a-zA-Z0-9_]+$  (字母、數字、下劃線，至少一位)
        val usernameRegex = "^[a-zA-Z0-9_]+$".toRegex()
        if (!username.matches(usernameRegex) || username.contains(" ")) {
            Log.w(
                TAG,
                "Sign up validation failed: Invalid username format for '$username'. Allowed: letters, numbers, underscore. No spaces."
            ) // 詳細日誌
            // 給用戶更明確的錯誤提示
            // authResultState = AuthResult.Error("用戶名只能包含字母、數字和下劃線")
            authResultState = AuthResult.Error("用戶名格式錯誤 (允許字母、數字、_)") // 簡化提示
            return
        }
        // 可選：用戶名長度驗證 (如果數據庫約束還不夠)
        if (username.length < 3 || username.length > 20) {
            Log.w(
                TAG,
                "Sign up validation failed: Username '$username' length (${username.length}) out of range (3-20)."
            )
            authResultState = AuthResult.Error("用戶名長度必須在 3 到 20 位之間")
            return
        }

        if (password.length < 6) { // Supabase 默認密碼最小長度為 6
            authResultState = AuthResult.Error("密碼長度至少為 6 位")
            return
        }
        if (password != confirmPassword) {
            authResultState = AuthResult.Error("兩次輸入的密碼不一致")
            return
        }

        // --- 構造內部使用的郵箱地址 ---
        val internalEmail = username + EMAIL_DOMAIN
        Log.d(TAG, "Constructed internal email: $internalEmail") // 記錄構造的郵箱

        viewModelScope.launch {
            authResultState = try {
                // 調用 Repository，傳遞構造好的 email 和原始 username
                val success = authRepository.signUp(
                    internalEmail,
                    password,
                    username
                ) // 假設 Repository signUp 不變
                if (success) {
                    Log.i(TAG, "Sign up successful for username: '$username'") // 成功日誌
                    _navigationEvent.emit(NavigationEvent.NavigateToMain)
                    AuthResult.Success
                } else {
                    Log.w(
                        TAG,
                        "Sign up failed (from repository) for username: '$username'. Likely username exists."
                    ) // 倉庫返回失敗
                    authResultState = AuthResult.Error("註冊失敗，用戶名可能已被使用") // 更新這裡的錯誤狀態
                    // 注意：如果之前已經 return 了錯誤狀態，這裡需要顯式賦值
                    // 因此將之前的 authResultState = ... 改為此行
                }
                authResultState // 返回最後確定的狀態

            } catch (e: Exception) {
                Log.e(TAG, "Sign up exception for username: '$username'", e) // 記錄異常及堆棧
                AuthResult.Error(e.message ?: "註冊過程中發生未知錯誤")
            }
        }
    }

    fun signIn() {
        authResultState = AuthResult.Loading
        Log.d(TAG, "Attempting to sign in with login input (username): '$loginInput'") // 記錄嘗試的登錄輸入
        // --- 修改登錄驗證和邏輯 ---
// (可選) 登錄時也可以簡單檢查一下用戶名格式，雖然主要依賴後端
        val usernameRegex = "^[a-zA-Z0-9_]+$".toRegex()
        if (!loginInput.matches(usernameRegex) || loginInput.contains(" ")) {
            Log.w(TAG, "Sign in validation failed: Invalid username format for login input '$loginInput'.")
            // 可以不直接返回錯誤，讓後端處理，但日誌有助於排查
            // authResultState = AuthResult.Error("用戶名格式錯誤")
            // return
        }

        if (password.isBlank()) {
            Log.w(TAG, "Sign in validation failed: Password is blank for login input '$loginInput'.")
            authResultState = AuthResult.Error("密碼不能為空")
            return
        }

        // --- 構造內部使用的郵箱地址 ---
        val internalEmail = loginInput + EMAIL_DOMAIN
        Log.d(TAG, "Constructed internal email for sign in: $internalEmail")

        viewModelScope.launch {
            Log.d(TAG, "Calling authRepository.signIn for internal email: $internalEmail (from login input '$loginInput')")
            authResultState = try {
                // 使用構造好的 email 調用 Repository 的 signIn
                val success = authRepository.signIn(internalEmail, password)
                if (success) {
                    Log.i(TAG, "Sign in successful for login input: '$loginInput'")
                    _navigationEvent.emit(NavigationEvent.NavigateToMain)
                    AuthResult.Success
                } else {
                    Log.w(TAG, "Sign in failed (from repository) for login input: '$loginInput'. Check username/password.")
                    AuthResult.Error("登錄失敗，請檢查用戶名和密碼") // 提示用戶檢查他們輸入的內容
                }

            } catch (e: Exception) {
                Log.e(TAG, "Sign in exception for login input: '$loginInput'", e)
                println("Sign in error: ${e.message}")
                AuthResult.Error(e.message ?: "登錄過程中發生未知錯誤") // 可以將 Supabase 錯誤轉換成更友好的提示
            }
        }
    }

    // 重置狀態，例如從註冊頁跳轉到登錄頁時清除狀態
    fun resetAuthState() {
        password = ""
        confirmPassword = ""
        username = ""
        passwordVisible = false
        confirmPasswordVisible = false
        authResultState = AuthResult.Idle
    }
}

// 定義導航事件
sealed class NavigationEvent {
    object NavigateToMain : NavigationEvent()
    object NavigateToLogin : NavigationEvent()
    object NavigateToRegister : NavigationEvent()
}