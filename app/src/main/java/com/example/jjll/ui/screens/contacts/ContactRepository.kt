package com.example.jjll.ui.screens.contacts

import android.service.autofill.Validators.and
import android.service.autofill.Validators.or
import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.Contact
import com.example.jjll.data.ContactStatus
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val profileRepository: ProfileRepository // 注入 ProfileRepository
) {

    companion object {
        private const val CONTACTS_TABLE = "contacts"
        private const val TAG = "ContactRepository"
    }

    /**
     * 获取收到的待处理好友请求的用户 Profile 列表
     * @param userId 当前用户的 ID (对应 contacts 表的 receiver_id 列)
     * @return List<UserProfile> 发送请求的用户信息列表
     */
    suspend fun getFriendRequests(userId: String): List<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching friend requests for receiverId: $userId")
            // 假設 contacts 表有 receiver_id 和 status 列
            val contacts = supabaseClient.postgrest[CONTACTS_TABLE]
                .select { // select 塊開始
                    filter { // filter 塊開始
                        eq("receiver_id", userId)
                        // 假設 status 列存儲的是枚舉的小寫名稱
                        eq("status", ContactStatus.PENDING.name.lowercase())
                    } // filter 塊結束
                } // select 塊結束
                .decodeList<Contact>() // 解碼為 Contact 列表

            Log.d(TAG, "Found ${contacts.size} pending contact entries.")

            val senderIds = contacts.map { it.senderId }
            if (senderIds.isEmpty()) {
                return@withContext emptyList()
            }

            // 使用 ProfileRepository 獲取發送者的 Profile
            val profiles = profileRepository.getProfiles(senderIds) // getProfiles 內部會處理 ID 列表查詢
            Log.d(TAG, "Fetched ${profiles.size} profiles for friend requests.")
            return@withContext profiles

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching friend requests for user $userId", e)
            return@withContext emptyList()
        }
    }

    /**
     * 获取已接受的联系人（好友）的 UserProfile 列表
     * @param userId 当前用户的 ID (在 contacts 表中可能是 sender_id 或 receiver_id)
     * @return List<UserProfile> 好友的用户信息列表
     */
    suspend fun getAcceptedContacts(userId: String): List<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching accepted contacts for userId: $userId")
            // 查詢 status 為 accepted 且當前用戶是 sender 或 receiver 的記錄
            // 假設 contacts 表有 status, sender_id, receiver_id 列
            val contacts = supabaseClient.postgrest[CONTACTS_TABLE]
                .select { // select 塊開始
                    filter { // filter 塊開始
                        eq("status", ContactStatus.ACCEPTED.name.lowercase())
                        or { // OR 條件組合
                            eq("sender_id", userId)
                            eq("receiver_id", userId)
                        }
                    } // filter 塊結束
                } // select 塊結束
                .decodeList<Contact>()

            Log.d(TAG, "Found ${contacts.size} accepted contact entries.")

            // 提取對方用戶的 ID
            val contactUserIds = contacts.mapNotNull { contact ->
                if (contact.senderId == userId) contact.receiverId
                else if (contact.receiverId == userId) contact.senderId
                else null
            }.distinct()

            if (contactUserIds.isEmpty()) {
                return@withContext emptyList()
            }

            // 獲取這些聯繫人的 Profile
            val profiles = profileRepository.getProfiles(contactUserIds)
            Log.d(TAG, "Fetched ${profiles.size} profiles for accepted contacts.")
            return@withContext profiles

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching accepted contacts for user $userId", e)
            return@withContext emptyList()
        }
    }

    /**
     * 发送好友请求（插入一条 pending 状态的记录）
     * @param senderId 发送者的 ID
     * @param receiverId 接收者的 ID
     * @return Boolean 是否成功发送请求
     */
    suspend fun addContact(senderId: String, receiverId: String): Boolean = withContext(Dispatchers.IO) {
        // 可選：檢查是否已存在關係（任何狀態）
        try {
            Log.d(TAG, "Checking existing contact before adding between $senderId and $receiverId")
            val existing = supabaseClient.postgrest[CONTACTS_TABLE]
                .select(columns = Columns.list("id")) { // 只查詢 id 列以提高效率
                    filter { // filter 塊開始
                        or { // 檢查兩種方向
                            and { // senderId -> receiverId
                                eq("sender_id", senderId)
                                eq("receiver_id", receiverId)
                            }
                            and { // receiverId -> senderId
                                eq("sender_id", receiverId)
                                eq("receiver_id", senderId)
                            }
                        }
                    } // filter 塊結束
                    limit(1) // 只需要知道是否存在
                }
                .decodeList<Map<String, Long>>() // 解碼為包含 id 的 Map 列表

            if (existing.isNotEmpty()) {
                Log.w(TAG, "Contact relationship already exists between $senderId and $receiverId. Cannot add new request.")
                return@withContext false // 或者拋出異常
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing contact before adding", e)
            // 根據策略決定是否繼續，這裡選擇繼續嘗試插入
        }

        // 插入新的 pending 請求
        try {
            val newContact = Contact(
                senderId = senderId,
                receiverId = receiverId,
                status = ContactStatus.PENDING
            )
            Log.d(TAG, "Adding new contact request from $senderId to $receiverId")
            supabaseClient.postgrest[CONTACTS_TABLE].insert(newContact)
            Log.i(TAG, "Successfully added contact request.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding contact from $senderId to $receiverId", e)
            return@withContext false
        }
    }

    /**
     * 接受好友请求 (更新状态为 accepted)
     * @param senderId 发送请求者的 ID
     * @param receiverId 接收请求者（当前用户）的 ID
     * @return Boolean 是否成功接受
     */
    suspend fun acceptFriendRequest(senderId: String, receiverId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Accepting friend request from $senderId for $receiverId")
            // 使用 update 更新指定記錄
            val result = supabaseClient.postgrest[CONTACTS_TABLE].update(
                { // update 塊：設置新值
                    var status = ContactStatus.ACCEPTED
                }
            ) { // filter 塊：指定更新哪一行
                filter{ eq("sender_id", senderId) }
                filter{ eq("receiver_id", receiverId) }
                filter{ eq("status", ContactStatus.PENDING.name.lowercase()) }
            }
            Log.i(TAG, "Friend request acceptance result: $result")
            return@withContext true // 假設無異常即成功
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request from $senderId for $receiverId", e)
            return@withContext false
        }
    }

    /**
     * 拒绝好友请求 (更新状态为 rejected)
     * @param senderId 发送请求者的 ID
     * @param receiverId 接收请求者（当前用户）的 ID
     * @return Boolean 是否成功拒绝
     */
    suspend fun rejectFriendRequest(senderId: String, receiverId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Rejecting friend request from $senderId for $receiverId")
            val result = supabaseClient.postgrest[CONTACTS_TABLE].update(
                { // update 塊：設置新值
                    var status = ContactStatus.REJECTED  //被拒絕
                }
            ) { // filter 塊：指定更新哪一行
                filter{ eq("sender_id", senderId) }
                filter{ eq("receiver_id", receiverId) }
                filter{ eq("status", ContactStatus.PENDING.name.lowercase()) }
            }
            Log.i(TAG, "Friend request rejection result: $result")
            return@withContext true

            /* // 或者選擇刪除記錄：
            supabaseClient.postgrest[CONTACTS_TABLE].delete { // filter 塊
                eq("sender_id", senderId)
                eq("receiver_id", receiverId)
                eq("status", ContactStatus.PENDING.name.lowercase())
            }
            Log.i(TAG, "Friend request deleted.")
            return@withContext true
            */
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request from $senderId for $receiverId", e)
            return@withContext false
        }
    }

    /**
     * (輔助函數，供 SearchUserDialog 使用)
     * 检查两个用户之间的关系状态
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return ContactStatus? 返回关系状态，如果不存在则返回 null
     */
    suspend fun getContactStatus(userId1: String, userId2: String): ContactStatus? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking contact status between $userId1 and $userId2")
            val contact = supabaseClient.postgrest[CONTACTS_TABLE]
                .select { // select 塊開始
                    filter { // filter 塊開始
                        or { // 檢查兩種方向
                            and {
                                eq("sender_id", userId1)
                                eq("receiver_id", userId2)
                            }
                            and {
                                eq("sender_id", userId2)
                                eq("receiver_id", userId1)
                            }
                        }
                    } // filter 塊結束
                    limit(1) // 只需找到一个匹配项
                } // select 塊結束
                .decodeSingleOrNull<Contact>() // 解碼單個對象

            Log.d(TAG, "Found contact status: ${contact?.status}")
            return@withContext contact?.status
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contact status between $userId1 and $userId2", e)
            return@withContext null // 出錯時返回 null
        }
    }
}
