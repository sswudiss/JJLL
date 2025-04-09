package com.example.jjll.ui.screens.contacts

import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ContactRepository"
private const val CONTACTS_TABLE = "contacts"
private const val PROFILES_TABLE = "profiles"


// --- 新增：用於表示待處理請求的數據結構 ---
@Serializable // 確保可序列化
data class PendingRequest(
    val contactId: Long, // contacts 表的 id，用於後續更新
    val senderProfile: UserProfile // 發送者的 Profile 信息
)

@Singleton
class ContactRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val auth get() = supabaseClient.auth
    private val postgrest get() = supabaseClient.postgrest

    // --- addContact 方法 (保持不變) ---
    suspend fun addContact(contactUserId: String): RepoResult<Boolean> {
        // ... (之前的實現) ...
        return withContext(Dispatchers.IO) {
            val currentUserId = auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                return@withContext RepoResult.Error("用戶未登錄")
            }
            if (currentUserId == contactUserId) {
                return@withContext RepoResult.Error("不能添加自己為聯繫人")
            }

            Log.d(TAG, "Attempting to add contact request from $currentUserId to $contactUserId")
            val newContactRequest =
                ContactRequestData(user_id = currentUserId, contact_user_id = contactUserId)
            try {
                postgrest[CONTACTS_TABLE].insert<ContactRequestData>(value = newContactRequest)
                Log.i(
                    TAG,
                    "Contact request sent successfully from $currentUserId to $contactUserId"
                )
                RepoResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding contact request from $currentUserId to $contactUserId", e)
                if (e.message?.contains("duplicate key value violates unique constraint") == true) {
                    RepoResult.Error("你已經發送過添加請求或已是好友")
                } else {
                    RepoResult.Error("發送聯繫人請求失敗: ${e.message}", e)
                }
            }
        }
    }


    // --- getAcceptedContacts 方法 (取得已接受聯絡人) ---
    suspend fun getAcceptedContacts(): RepoResult<List<UserProfile>> {
        // ... (之前的實現，查詢 contacts 获取对方 IDs，再查询 profiles) ...
        return withContext(Dispatchers.IO) {
            val currentUserId =
                auth.currentUserOrNull()?.id ?: return@withContext RepoResult.Error("用戶未登錄")
            Log.d(TAG, "Fetching accepted contacts for user: $currentUserId")
            try {
                //第一步查詢和解碼
                val contactIdsResult = postgrest[CONTACTS_TABLE]
                    .select(
                        columns = Columns.list(
                            "id",
                            "user_id",
                            "contact_user_id"
                        )
                    ) { // 可能需要 id 來刪除
                        filter {
                            // 在 filter 內部使用 or 來組合 OR 條件
                            or {
                                eq("user_id", currentUserId)
                                eq("contact_user_id", currentUserId)
                            }
                            // and 條件直接寫在 filter lambda 內 (or 之外)
                            eq("status", "accepted")
                        }
                    }.decodeList<ContactIdPair>() // <--- 解碼是否成功？ contactIdsResult 是否包含了預期的 ID 對？在這裡加日誌打印 contactIdsResult 的內容和大小。
                Log.i(TAG, "Successfully fetched ${contactIdsResult.size} accepted contact profiles")

                //檢查提取對方 ID 的邏輯
                val otherUserIds =
                    contactIdsResult.map { if (it.user_id == currentUserId) it.contact_user_id else it.user_id }
                        .distinct()  // <--- 這一步提取出的 otherUserIds 列表是否包含了對方的正確 UID？加日誌打印 otherUserIds。
                Log.i(TAG, "Successfully fetched ${otherUserIds.size} accepted contact profiles")
                if (otherUserIds.isEmpty()) return@withContext RepoResult.Success(emptyList())

                //第二步查詢和解碼：
                val profiles = postgrest[PROFILES_TABLE].select {
                    filter { isIn("user_id", otherUserIds) }
                }.decodeList<UserProfile>()  // <--- 最終的 profiles 列表是否包含了對方的 UserProfile？加日誌打印 profiles 的大小和內容。
                Log.i(TAG, "Successfully fetched ${profiles.size} accepted contact profiles")
                RepoResult.Success(profiles)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching accepted contacts", e)
                RepoResult.Error("獲取聯繫人列表失敗: ${e.message}", e)
            }
        }
    }

    // --- 新增：獲取待處理的收到的好友請求 ---
    suspend fun getPendingRequests(): RepoResult<List<PendingRequest>> {
        return withContext(Dispatchers.IO) {
            val currentUserId =
                auth.currentUserOrNull()?.id ?: return@withContext RepoResult.Error("用戶未登錄")

            Log.d(TAG, "Fetching pending requests for user: $currentUserId")

            try {
                // 1. 查詢發給我的、狀態為 pending 的 contacts 記錄，獲取 contactId 和 發送者 user_id
                val pendingContacts = postgrest[CONTACTS_TABLE]
                    .select(columns = Columns.list("id", "user_id")) { // 獲取 contact id 和 發送者 id
                        filter {
                            eq("contact_user_id", currentUserId) // 我是被添加者
                            eq("status", "pending")              // 狀態是 pending
                        }
                    }
                    .decodeList<PendingContactInfo>() // 解碼到臨時 data class

                if (pendingContacts.isEmpty()) {
                    Log.i(TAG, "No pending requests found for user: $currentUserId")
                    return@withContext RepoResult.Success(emptyList())
                }

                val senderIds = pendingContacts.map { it.user_id }
                Log.d(TAG, "Found ${senderIds.size} pending requests from user IDs: $senderIds")

                // 2. 根據發送者 ID 列表查詢他們的 profiles
                val senderProfiles = postgrest[PROFILES_TABLE]
                    .select {
                        filter { isIn("user_id", senderIds) }
                    }
                    .decodeList<UserProfile>()
                    .associateBy { it.user_id } // 將列表轉換為 Map<UserId, UserProfile> 以便快速查找

                // 3. 組裝 PendingRequest 列表
                val pendingRequests = pendingContacts.mapNotNull { contactInfo ->
                    senderProfiles[contactInfo.user_id]?.let { senderProfile ->
                        PendingRequest(
                            contactId = contactInfo.id,
                            senderProfile = senderProfile
                        )
                    }
                }

                Log.i(
                    TAG,
                    "Successfully assembled ${pendingRequests.size} pending requests details"
                )
                RepoResult.Success(pendingRequests)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching pending requests for user $currentUserId", e)
                RepoResult.Error("獲取好友請求失敗: ${e.message}", e)
            }
        }
    }

    // --- 新增：更新聯繫人請求狀態 ---
    suspend fun updateContactStatus(contactId: Long, newStatus: String): RepoResult<Boolean> {
        return withContext(Dispatchers.IO) {
            val currentUserId =
                auth.currentUserOrNull()?.id ?: return@withContext RepoResult.Error("用戶未登錄")
            // RLS 策略會確保只有接收者能更新 pending 狀態

            if (newStatus != "accepted" && newStatus != "blocked") { // 簡單驗證狀態
                return@withContext RepoResult.Error("無效的狀態更新")
            }

            Log.d(
                TAG,
                "Attempting to update contact request (id: $contactId) to status: $newStatus by user $currentUserId"
            )

            try {
                postgrest[CONTACTS_TABLE]
                    .update({ set("status", newStatus) }) {
                        filter {
                            eq("id", contactId)
                            // 可以選擇添加 `eq("contact_user_id", currentUserId)` 來增加客戶端檢查
                        }
                    }
                Log.i(
                    TAG,
                    "Successfully updated contact request (id: $contactId) to status: $newStatus"
                )
                RepoResult.Success(true)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error updating contact request (id: $contactId) to status $newStatus",
                    e
                )
                RepoResult.Error("更新請求狀態失敗: ${e.message}", e)
            }
        }
    }

    // --- 可能需要的輔助 Data Classes ---
    @Serializable
    private data class ContactIdPair( // 根據需要調整，可能需要 id
        val id: Long? = null, // 添加 id 以便刪除或更新
        val user_id: String,
        val contact_user_id: String
    )

    @Serializable
    private data class ContactRequestData(val user_id: String, val contact_user_id: String)

    @Serializable
    private data class PendingContactInfo(
        val id: Long,
        val user_id: String
    ) // 用於獲取 pending 請求的 ID 和發送者 ID
}