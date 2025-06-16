package com.example.jjll.data.repository

import android.util.Log
import com.example.jjll.data.model.FriendRequest
import com.example.jjll.data.model.Friendship
import com.example.jjll.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class FriendRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FriendRepository {

    private val profilesTable = "profiles"
    private val friendRequestsTable = "friend_requests"
    private val friendshipsTable = "friendships"

    /**
     * 搜索用戶
     */
    override suspend fun searchUsers(
        query: String,
        currentUserId: String,
        limit: Int
    ): Result<List<Profile>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }
            val result = supabaseClient.postgrest[profilesTable].select {
                filter {
                    or {
                        ilike("username", "%$query%")
                        ilike("display_name", "%$query%")
                    }
                    neq("user_id", currentUserId)
                }

                limit(limit.toLong())
            }.decodeList<Profile>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error searching users", e)
            Result.failure(e)
        }
    }

    /**
     * 發送好友請求
     */
    override suspend fun sendFriendRequest(receiverId: String, message: String): Result<Unit> {
        return try {
            // 1. 獲取當前登錄用戶的 ID，如果未登錄則返回錯誤
            val requesterId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(IllegalStateException("User not logged in."))

            // 2. 構建要插入的數據對象 (使用 Map)
            val newRequest = mapOf(
                "requester_id" to requesterId,
                "receiver_id" to receiverId,
                "message" to message,
                "status" to "pending"
            )
            // 直接調用 insert，不帶任何額外的 lambda 配置
            // 因為默認的 returning 行為就是 Minimal
            supabaseClient.postgrest[friendRequestsTable].insert(
                value = newRequest
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error sending friend request", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取收到的好友請求，並附帶請求者的 Profile 信息
     */
    override suspend fun getReceivedFriendRequests(userId: String): Result<List<FriendRequest>> {
        return try {
            // 修正 JOIN 查詢語法
            // 語法：foreign_table!foreign_key_column(selected_columns)
            // 在這裡，我們告訴 PostgREST：使用 friend_requests 表上的 requester_id 列來 JOIN profiles 表
            //使用 * 的好處是，即使你以後給 profiles 表添加了新的列，並且在 Profile.kt 中也添加了對應的非空屬性，你也不需要回來修改這個查詢字符串。
            val queryColumns = Columns.raw("*, requester_profile:profiles!requester_id(*)")  // <--- 使用 *

            val result = supabaseClient.postgrest[friendRequestsTable].select(
                columns = queryColumns
            ) {
                filter {
                    eq("receiver_id", userId)
                    eq("status", "pending")
                }
            }.decodeList<FriendRequest>()

            Result.success(result)
        } catch (e: Exception) {
            // 現在的日誌會捕獲新的錯誤（如果有的話）
            Log.e("FriendRepository", "Error getting received friend requests: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 接受好友請求
     */
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[friendRequestsTable].update(
                update = { set("status", "accepted") }
            ) {
                filter {
                    eq("id", requestId)
                }
            }
            // 觸發器會自動創建 friendship
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    /**
     * 拒絕好友請求
     */
    override suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[friendRequestsTable].update(
                update = { set("status", "declined") }
            ) {
                filter {
                    eq("id", requestId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error declining friend request", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取好友列表
     */
    override suspend fun getFriends(userId: String): Result<List<Friendship>> {
        return try {
            val friendships = supabaseClient.postgrest[friendshipsTable].select {
                filter {
                    or {
                        eq("user1_id", userId)
                        eq("user2_id", userId)
                    }
                }
            }.decodeList<Friendship>()

            // 獲取所有好友的 ID
            val friendIds = friendships.map {
                if (it.user1Id == userId) it.user2Id else it.user1Id
            }

            if (friendIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // 根據好友 ID 批量獲取他們的 Profile
            val friendProfiles = supabaseClient.postgrest[profilesTable].select {
                filter {
                    isIn("user_id", friendIds)
                }
            }.decodeList<Profile>()
                .associateBy { it.userId } // 將 Profile 列表轉換為以 userId 為鍵的 Map，方便查找

            // 將 Profile 信息填充到 Friendship 對象中
            val result = friendships.mapNotNull { friendship ->
                val friendId =
                    if (friendship.user1Id == userId) friendship.user2Id else friendship.user1Id
                friendProfiles[friendId]?.let { profile ->
                    friendship.apply { friendProfile = profile }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error getting friends", e)
            Result.failure(e)
        }
    }
}