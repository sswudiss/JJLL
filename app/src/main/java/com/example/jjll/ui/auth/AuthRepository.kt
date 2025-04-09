package com.example.jjll.ui.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton

//創建一個 Repository 層是良好的 Android 架構實踐。
// 它將數據操作（在這裡是與 Supabase Auth 和 DB 的交互）從 ViewModel 中分離出來

@Singleton // 通常 Repository 是單例
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    // 表名常量
    private val PROFILES_TABLE = "profiles"

    // 修改後的 AuthRepository.signUp
    suspend fun signUp(email: String, password: String, username: String): Boolean { // username 參數可能仍需要，如果要在 metadata 中傳遞
        return try {
            Log.d("AuthRepo", "Attempting Auth sign up for email: $email")
            // 將 username 存入 metadata (如果觸發器要從 metadata 讀取)
            /* val userMetaData = buildJsonObject { put("username", username) } */

            val user = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // this.data = userMetaData // 傳遞 metadata
            }

            // 只需要檢查 Auth User 是否創建成功
            if (user != null && user.id != null) {
                Log.i("AuthRepo", "Auth sign up successful for email: $email. Profile will be created by trigger.")
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

}


