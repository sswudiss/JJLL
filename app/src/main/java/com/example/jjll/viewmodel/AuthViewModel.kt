package com.example.jjll.viewmodel // 替換成你的包名

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException // 可以捕獲更通用的 Supabase 異常
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    init {
        // 這是主要的會話狀態監聽器
        supabaseClient.auth.sessionStatus
            .onEach { status ->
                _authState.update {
                    it.copy(
                        isAuthenticated = status is SessionStatus.Authenticated,
                        isLoading = status is SessionStatus.Initializing,
                        session = if (status is SessionStatus.Authenticated) status.session else null
                    )
                }
                if (status is SessionStatus.Authenticated) {
                    // UserSession 包含 UserInfo
                    // status.session (類型是 UserSession) 有一個 'user' 屬性，其類型應該是 UserInfo
                    _currentUser.value = status.session.user
                } else {
                    _currentUser.value = null
                }
            }
            .launchIn(viewModelScope)

        // 初始會話加載邏輯 sessionStatus flow 已經處理了初始加載
    }

    private fun formatEmailFromUsername(username: String): String {
        // 確保用戶名本身不包含 "@" 等特殊字符，或者進行清理
        val cleanUsername =
            username.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '-' } // 簡單清理示例
        if (cleanUsername.isBlank()) {
            throw IllegalArgumentException("用戶名不能為空或僅包含無效字符")
        }
        return "${cleanUsername}@jjll.com" // 你的固定域名
    }

    // 封裝錯誤處理邏輯
    private fun handleAuthError(e: Exception, defaultUserMessage: String) {
        Log.e("AuthViewModel", "Auth Error Type: ${e::class.java.simpleName}")
        Log.e("AuthViewModel", "Auth Error Message: ${e.message}")
        // e.printStackTrace() // 在調試時可以保留，發布時可以考慮移除或用級別更低的 Log.d

        val userFriendlyMessage = when (e) {
            is AuthRestException -> { // <--- 捕獲更精確的 AuthRestException
                Log.d(
                    "AuthViewModel",
                    "AuthRestException caught. Message: ${e.message}, Error: ${e.error}, Description: ${e.errorDescription}"
                )
                // AuthRestException 可能有更結構化的字段，如 e.error 或 e.errorDescription
                // 根據日誌，e.message 包含了 "user_already_exists"
                when {
                    // 直接判斷 message 中是否包含這個關鍵的錯誤碼/標識符
                    e.message?.contains("user_already_exists", ignoreCase = true) == true ->
                        "此用戶名已被使用，請嘗試其他用戶名。"
                    // 你可以根據 AuthRestException 的其他潛在 message 內容添加更多 case
                    // 例如，對於密碼錯誤，也可能是 AuthRestException，message 中包含 "Invalid login credentials"
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "用戶名或密碼錯誤，請重試。"
                    // Supabase 的密碼策略錯誤通常也是 AuthRestException
                    e.message?.contains("Password should be at least", ignoreCase = true) == true ||
                            e.message?.contains(
                                "Password is too short",
                                ignoreCase = true
                            ) == true ->
                        "密碼長度至少需要6個字符。"
                    // 如果 AuthRestException 有 e.error 字段，並且它就是 "user_already_exists" 也可以這樣判斷
                    // e.error == "user_already_exists" -> "此用戶名已被使用..."
                    else -> {
                        Log.w("AuthViewModel", "Unhandled AuthRestException: ${e.message}")
                        defaultUserMessage // 對於未明確處理的 AuthRestException，使用默認消息
                    }
                }
            }

            is HttpRequestException -> { // 處理網絡等問題
                "網絡連接失敗，請檢查你的網絡後重試。"
            }

            else -> defaultUserMessage // 其他未知異常
        }
        _authState.update { it.copy(isLoading = false, error = userFriendlyMessage) }
    }


    fun signUpWithUsername(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                if (usernameInput.isBlank()) {
                    _authState.update { it.copy(isLoading = false, error = "用戶名不能為空") }
                    return@launch
                }
                // 密碼長度檢查 (可以在前端也做，但後端也應該有)
                if (passwordInput.length < 6) {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = "密碼長度至少需要6個字符。"
                        )
                    }
                    return@launch
                }

                val emailForSupabase = formatEmailFromUsername(usernameInput)
                supabaseClient.auth.signUpWith(Email) {
                    email = emailForSupabase
                    password = passwordInput
                    //JsonPrimitive() 構造函數可以接受 String, Number, 或 Boolean，並將它們轉換為相應的 JSON 基本類型。
                    data = buildJsonObject {
                        put("username", JsonPrimitive(usernameInput.trim()))
                    }
                }
                _authState.update { it.copy(isLoading = false) } // 成功後清除錯誤
            } catch (e: Exception) {
                handleAuthError(e, "創建賬戶失敗，請稍後重試。")
            }
        }
    }

    fun signInWithUsername(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                if (usernameInput.isBlank()) {
                    _authState.update { it.copy(isLoading = false, error = "用戶名不能為空") }
                    return@launch
                }
                val emailForSupabase = formatEmailFromUsername(usernameInput)
                supabaseClient.auth.signInWith(Email) {
                    email = emailForSupabase
                    password = passwordInput
                }
                _authState.update { it.copy(isLoading = false) } // 成功後清除錯誤
            } catch (e: Exception) {
                handleAuthError(e, "登錄失敗，請檢查你的用戶名和密碼。")
            }
        }
    }

    fun signOut() { // signOut 通常不太會拋出需要用戶特別關注的錯誤，但也可以加上
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                supabaseClient.auth.signOut()
                _authState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out error: ${e.message}", e)
                // 登出失敗通常不需要在 UI 上顯示複雜的錯誤，可以選擇不更新 error
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = "登出時發生問題。"
                    )
                } // 或不設置 error
            }
        }
    }

    fun clearAuthError() {
        _authState.update { it.copy(error = null) }
    }

    suspend fun getInitialAuthStatus(): Boolean {
        // 修正: 等待第一個非 Initializing 的狀態
        val status = supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return status is SessionStatus.Authenticated
    }

    //獲取並上傳 Token
    //在用戶登錄成功後，獲取 FCM token 並將其保存到該用戶的 profiles 表中。
    fun uploadFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Current token: $token")
            // 在這裡調用你的 Repository 方法將 token 上傳到 Supabase
            // viewModelScope.launch { profileRepository.updateFcmToken(token) }
        }
    }
}