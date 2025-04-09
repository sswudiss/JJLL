package com.example.jjll.data

import android.util.Log
import com.example.jjll.common.RepoResult
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "ProfileRepository"
private const val PROFILES_TABLE = "profiles"

//這個 Repository 將專門負責與 profiles（個人資料） 表相關的數據操作。
@Singleton
class ProfileRepository @Inject constructor(
    private val postgrest: Postgrest // 直接注入 Postgrest client
) {

    /**
     * 根據 User ID 獲取用戶 Profile 信息
     */
    suspend fun getUserProfile(userId: String): RepoResult<UserProfile> {
        // 使用 IO Dispatcher 執行網絡請求
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching profile for userId: $userId")
                val profile = postgrest[PROFILES_TABLE]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserProfile>() // 嘗試只獲取一個匹配的記錄

                if (profile != null) {
                    Log.i(TAG, "Profile found for userId $userId: ${profile.username}")
                    RepoResult.Success(profile)
                } else {
                    Log.w(TAG, "Profile not found for userId: $userId")
                    RepoResult.Error("未能找到用戶資料") // 或者可以返回成功但數據為空 RepoResult.Success(null) ? 取決於ViewModel期望
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profile for userId $userId", e)
                // 可以根據 e 的類型提供更具體的錯誤消息
                RepoResult.Error("獲取用戶資料失敗: ${e.message}", e)
            }
        }
    }

    /**
     * 更新用戶的 Display Name
     * (後續實現)
     */
    suspend fun updateDisplayName(userId: String, newDisplayName: String): RepoResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating display name for userId: $userId to '$newDisplayName'")
                postgrest[PROFILES_TABLE]
                    .update( { set("display_name", newDisplayName) } ) { // 使用 update lambda
                        filter { eq("user_id", userId) }
                    }
                Log.i(TAG, "Display name updated successfully for userId: $userId")
                RepoResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating display name for userId $userId", e)
                RepoResult.Error("更新顯示名稱失敗: ${e.message}", e)
            }
        }
    }

    /**
     * 更新用戶的 Avatar URL
     * (後續實現)
     */
    suspend fun updateAvatarUrl(userId: String, newAvatarUrl: String?): RepoResult<Boolean> {
        // 類似 updateDisplayName，更新 avatar_url 字段
        return RepoResult.Success(true) // 佔位符
    }

    // 可以添加其他與 Profile 相關的操作，例如按用戶名搜索
    suspend fun searchUsersByUsername(query: String): RepoResult<List<UserProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching users by username query: '$query'")
                // 使用 ilike 進行不區分大小寫的部分匹配，並限制數量
                val results = postgrest[PROFILES_TABLE]
                    .select {
                        filter { ilike("username", "%${query}%") } // % 是通配符
                        limit(10) // 限制返回結果數量
                    }
                    .decodeList<UserProfile>()
                Log.i(TAG, "Found ${results.size} users matching query '$query'")
                RepoResult.Success(results)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users by username query '$query'", e)
                RepoResult.Error("搜索用戶失敗: ${e.message}", e)
            }
        }
    }

}