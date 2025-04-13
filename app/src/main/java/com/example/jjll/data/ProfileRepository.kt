package com.example.jjll.data

import android.util.Log
import com.example.jjll.ui.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


//個人資料儲存庫
// 包含了獲取用戶信息、搜索用戶以及更新用戶信息等常用功能
@Singleton
class ProfileRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {

    companion object {
        private const val PROFILES_TABLE = "profiles"
        private const val TAG = "ProfileRepository"
    }

    /**
     * 获取指定用户的 Profile
     * @param userIdToGet 要获取 Profile 的用户 ID (对应 profiles 表的 'id' 列)
     * @return UserProfile? 如果找到则返回 Profile，否则返回 null
     */
    suspend fun getProfile(userIdToGet: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userIdToGet.isBlank()) {
            Log.w(TAG, "getProfile called with blank userId.")
            return@withContext null
        }
        try {
            Log.d(TAG, "Fetching profile for userId: $userIdToGet")
            // 假設數據庫中關聯 Auth 的列名是 'id'
            val profile = supabaseClient.postgrest[PROFILES_TABLE]
                .select {
                    filter { eq("id", userIdToGet) }
                }
                .decodeSingleOrNull<UserProfile>() // 解碼為您定義的 UserProfile

            if (profile == null) {
                Log.w(TAG, "Profile not found for userId: $userIdToGet")
            } else {
                // 使用您 UserProfile 中的字段名 userId
                Log.d(TAG, "Profile found for userId ${profile.userId}: ${profile.username}")
            }
            return@withContext profile
        } catch (e: NotFoundRestException) {
            Log.w(TAG, "Profile not found for userId $userIdToGet (NotFoundRestException)")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile for userId $userIdToGet", e)
            return@withContext null
        }
    }

    /**
     * 获取多个用户的 Profile (供 ContactRepository 使用)
     * @param userIds 用户 ID 列表 (对应 profiles 表的 'id' 列)
     * @return List<UserProfile> 找到的 Profile 列表
     */
    suspend fun getProfiles(userIds: List<String>): List<UserProfile> =
        withContext(Dispatchers.IO) {
            if (userIds.isEmpty()) {
                Log.d(TAG, "getProfiles called with empty list.")
                return@withContext emptyList()
            }
            try {
                Log.d(TAG, "Fetching profiles for ${userIds.size} users.")
                // 假設數據庫中關聯 Auth 的列名是 'id'
                val profiles = supabaseClient.postgrest[PROFILES_TABLE]
                    .select {
                        filter { isIn("id", userIds) }
                    }
                    .decodeList<UserProfile>() // 解碼為您定義的 UserProfile 列表
                Log.d(TAG, "Fetched ${profiles.size} profiles.")
                return@withContext profiles
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profiles for userIds: $userIds", e)
                return@withContext emptyList()
            }
        }


    /**
     * 获取当前登录用户的 Profile
     * @return UserProfile? 如果成功获取则返回 Profile，否则返回 null
     */
    suspend fun getCurrentUserProfile(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.w(TAG, "Cannot get current user profile, user not logged in.")
                return@withContext null
            }
            // currentUserId 是從 Auth 獲取的 ID，對應 profiles 表的 'id' 列
            return@withContext getProfile(currentUserId) // 複用 getProfile 方法
        } catch (e: JJLLAuthException) {
            Log.e(TAG, "Auth error getting current user ID", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting current user profile", e)
            return@withContext null
        }
    }

    /**
     * 根据用户名搜索用户 Profile (排除当前用户)
     * @param query 搜索关键词 (部分用户名)
     * @param limit 结果数量限制
     * @return List<UserProfile> 匹配的用户 Profile 列表
     */
    suspend fun searchProfiles(query: String, limit: Int = 10): List<UserProfile> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext emptyList()
            }
            try {
                val currentUserId = authRepository.getCurrentUserId()
                Log.d(
                    TAG,
                    "Searching profiles with query: '$query', excluding user: $currentUserId"
                )

                val profiles = supabaseClient.postgrest[PROFILES_TABLE]
                    .select { // select 塊開始
                        filter { // filter 塊開始
                            // 假設數據庫的用戶名列是 'username'
                            ilike("username", "%$query%")
                            // 排除當前用戶，假設 ID 列是 'id'
                            if (currentUserId != null) {
                                neq("id", currentUserId)
                            }
                        } // filter 塊結束
                        // 可以在 filter 塊外部設置 limit, order 等
                        limit(limit.toLong())
                        // order("username", Order.ASCENDING) // 示例：按用戶名排序
                    } // select 塊結束
                    .decodeList<UserProfile>() // 解碼為您定義的 UserProfile 列表

                Log.d(TAG, "Found ${profiles.size} profiles matching query '$query'.")
                return@withContext profiles
            } catch (e: JJLLAuthException) {
                Log.e(TAG, "Auth error getting current user ID during search", e)
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error searching profiles with query '$query'", e)
                return@withContext emptyList()
            }
        }

    /**
     * 更新当前用户的 Profile 信息
     * @param profileUpdate 包含要更新字段的对象
     * @return Boolean 是否更新成功
     */
    suspend fun updateProfile(profileUpdate: ProfileUpdate): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.w(TAG, "Cannot update profile, user not logged in.")
                throw JJLLAuthException("User not logged in to update profile.")
            }

            Log.d(TAG, "Updating profile for user: $currentUserId with data: $profileUpdate")

            // update 函數的過濾部分是在 update 塊之後的 lambda 中直接調用 eq 等
            val result = supabaseClient.postgrest[PROFILES_TABLE].update(
                { // update 塊，定義要更新的字段
                    // 使用數據庫列名
                    if (profileUpdate.username != null) {
                        set("username", profileUpdate.username) // 假設列名 username
                    }
                    if (profileUpdate.displayName != null) {
                        set("display_name", profileUpdate.displayName) // 假設列名 display_name
                    }
                    if (profileUpdate.avatarURL != null) {
                        set("avatar_url", profileUpdate.avatarURL) // 假設列名 avatar_url
                    }
                    // set("updated_at", "now()") // 可選
                }
            ) { // filter 塊，定義更新哪一行
                // 直接調用 eq 指定 'id' 列
                filter { eq("id", currentUserId) }// 假設 ID 列是 'id'
            }

            Log.i(TAG, "Profile update result for user $currentUserId")
            return@withContext true // 假設無異常即成功

        } catch (e: JJLLAuthException) {
            Log.e(TAG, "Auth error during profile update", e)
            return@withContext false
        } catch (e: BadRequestRestException) {
            Log.e(TAG, "Bad request during profile update (e.g., duplicate username?)", e)
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            return@withContext false
        }
    }
}