package com.example.jjll.ui.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton




import com.example.jjll.data.JJLLAuthException // 導入自定義異常
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession

import io.github.jan.supabase.exceptions.BadRequestRestException

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


//創建一個 Repository 層是良好的 Android 架構實踐。
// 它將數據操作（在這裡是與 Supabase Auth 和 DB 的交互）從 ViewModel 中分離出來

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val TAG = "AuthRepository"
        // 定義虛構郵箱後綴 (應與 ViewModel 中使用的保持一致)
        // 可以考慮將其定義在共享的常量文件中
        const val INTERNAL_EMAIL_SUFFIX = "@jjll.org"
    }

    /**
     * 註冊新用戶 (內部使用虛構郵箱)。
     * @param username 用戶選擇的唯一用戶名
     * @param password 用戶密碼
     * @throws JJLLAuthException 如果註冊失敗
     */
    suspend fun registerWithUsername(username: String, password: String) = withContext(Dispatchers.IO) {
        val internalEmail = "$username$INTERNAL_EMAIL_SUFFIX" // 構建內部郵箱
        try {
            Log.d(TAG, "嘗試使用內部郵箱 $internalEmail (對應用戶名 $username) 註冊...")
            // 使用 Email 提供者進行註冊
            supabaseClient.auth.signUpWith(Email) {
                this.email = internalEmail
                this.password = password
                // 注意：這裡不傳遞 data，假設觸發器直接使用 username 創建 profile
                // 如果觸發器需要 username，可以在 data 中傳遞:
                // this.data = mapOf("username" to username)
            }
            Log.i(TAG, "用戶 $username (郵箱 $internalEmail) 的 Auth 賬號創建成功。")
            // 註冊成功，無需返回值，失敗會拋異常
        } catch (e: Exception) {
            Log.e(TAG, "註冊失敗 for username $username (email $internalEmail)", e)
            val errorMessage = when (e) {
                is BadRequestRestException -> {
                    // 判斷錯誤原因，可能是用戶名(郵箱)已存在或密碼太弱
                    // 這裡的錯誤信息可能需要根據 GoTrue 的實際返回調整
                    if (e.message?.contains("User already registered", ignoreCase = true) == true ||
                        e.message?.contains("already exists", ignoreCase = true) == true) { // GoTrue 可能返回不同錯誤
                        "用戶名 '$username' 已被註冊。"
                    } else if (e.message?.contains("Password should be at least", ignoreCase = true) == true) {
                        "密碼太弱，請設置更複雜的密碼。"
                    } else {
                        "註冊時發生錯誤，請檢查用戶名和密碼。" // 通用錯誤
                    }
                }
                else -> "註冊時發生未知錯誤: ${e.localizedMessage}"
            }
            throw JJLLAuthException(errorMessage, e)
        }
    }

    /**
     * 使用用戶名和密碼登錄 (內部轉換為虛構郵箱)。
     * @param username 用戶輸入的用戶名
     * @param password 用戶密碼
     * @throws JJLLAuthException 如果登錄失敗
     */
    suspend fun loginWithUsername(username: String, password: String) = withContext(Dispatchers.IO) {
        val internalEmail = "$username$INTERNAL_EMAIL_SUFFIX" // 構建內部郵箱
        try {
            Log.d(TAG, "嘗試使用內部郵箱 $internalEmail (對應用戶名 $username) 登錄...")
            supabaseClient.auth.signInWith(Email) {
                this.email = internalEmail
                this.password = password
            }
            Log.i(TAG, "用戶 $username (郵箱 $internalEmail) 登錄成功。")
        } catch (e: Exception) {
            Log.e(TAG, "登錄失敗 for username $username (email $internalEmail)", e)
            val errorMessage = when (e) {
                is BadRequestRestException -> "用戶名或密碼錯誤。" // 對用戶來說，通常是憑據無效
                else -> "登錄時發生錯誤: ${e.localizedMessage}"
            }
            throw JJLLAuthException(errorMessage, e)
        }
    }

    /**
     * 登出當前用戶。
     * @throws JJLLAuthException 如果登出過程中發生錯誤
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "正在登出...")
            supabaseClient.auth.signOut()
            Log.i(TAG, "用戶已成功登出。")
        } catch (e: Exception) {
            Log.e(TAG, "登出時發生錯誤", e)
            throw JJLLAuthException("登出時發生錯誤: ${e.message}", e)
        }
    }

    /**
     * 獲取當前登錄用戶的 ID (UUID 字符串)。
     * @return String? 如果用戶已登錄則返回用戶 ID，否則返回 null。
     */
    fun getCurrentUserId(): String? {
        val userId = try {
            supabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "獲取 currentUserOrNull 時發生意外錯誤", e)
            null
        }
        Log.d(TAG, "獲取當前用戶 ID: $userId")
        return userId
    }

    /**
     * 獲取當前用戶會話信息。
     * @return UserSession? 如果用戶已登錄則返回會話，否則返回 null。
     */
    fun getCurrentSession(): UserSession? {
        return try {
            supabaseClient.auth.currentSessionOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "獲取 currentSessionOrNull 時發生意外錯誤", e)
            null
        }
    }

    /**
     * 提供一個 Flow 來觀察身份驗證狀態的變化。
     * @return Flow<SessionStatus>
     */
    fun observeSessionStatus(): Flow<SessionStatus> {
        return supabaseClient.auth.sessionStatus
    }
}



/*
@Singleton // 通常 Repository 是單例
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    // 表名常量
    private val PROFILES_TABLE = "profiles"

    // 修改後的 AuthRepository.signUp
    suspend fun signUp(
        email: String,
        password: String,
        username: String
    ): Boolean { // username 參數可能仍需要，如果要在 metadata 中傳遞
        return try {
            Log.d("AuthRepo", "Attempting Auth sign up for email: $email")
            // 將 username 存入 metadata (如果觸發器要從 metadata 讀取)
            *//* val userMetaData = buildJsonObject { put("username", username) } *//*

            val user = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // this.data = userMetaData // 傳遞 metadata
            }

            // 只需要檢查 Auth User 是否創建成功
            if (user != null) {
                Log.i(
                    "AuthRepo",
                    "Auth sign up successful for email: $email. Profile will be created by trigger."
                )
                true // 返回 true，讓 ViewModel 認為註冊成功
            } else {
                Log.e("AuthRepo", "Auth sign up failed or returned null user/id for email: $email")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Exception during Auth sign up for email: $email", e)
            false
        }
    }

    //signIn 函數接收由 ViewModel 構造的 internalEmail
    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email   //使用傳入的 internalEmail (e.g., loginInput@jjll.org)
                this.password = password
            }
            println("User signed in successfully with email: $email")
            // signIn 會自動處理 Session 持久化
            true
        } catch (e: Exception) {
            println("Error during sign in: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
            println("User signed out successfully.")
        } catch (e: Exception) {
            println("Error during sign out: ${e.message}")
            e.printStackTrace()
        }
    }

    // 可能需要的輔助函數，比如獲取當前用戶信息
    fun getCurrentUserId(): String? {
        // 注意：這需要檢查 session 狀態
        val session = supabaseClient.auth.currentSessionOrNull()
        return session?.user?.id
    }
}*/


