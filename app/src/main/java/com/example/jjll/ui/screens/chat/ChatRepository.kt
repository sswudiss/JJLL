package com.example.jjll.ui.screens.chat


import android.util.Log
import com.example.jjll.data.ChatListItem
import com.example.jjll.data.LastMessagePreview
import com.example.jjll.data.Message
import com.example.jjll.data.ProfileRepository
import com.example.jjll.data.RealtimeState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val profileRepository: ProfileRepository
) {

    companion object {
        private const val MESSAGES_TABLE = "messages"
        private const val TAG = "ChatRepository"
    }

    // Realtime 相關
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var chatChannel: RealtimeChannel? = null
    private val _realtimeStatus = MutableStateFlow<RealtimeState>(RealtimeState.Closed)

    // --- 消息相關方法 ---

    suspend fun getMessagesBetweenUsers(user1Id: String, user2Id: String): List<Message> = withContext(Dispatchers.IO) {
        // ... (實現與之前版本相同) ...
        Log.d(TAG, "正在獲取用戶 $user1Id 和 $user2Id 之間的歷史消息")
        try {
            val messages = supabaseClient.postgrest[MESSAGES_TABLE]
                .select {
                    filter {
                        or {
                            and { eq("sender_id", user1Id); eq("receiver_id", user2Id) }
                            and { eq("sender_id", user2Id); eq("receiver_id", user1Id) }
                        }
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Message>()
            Log.d(TAG, "成功獲取 ${messages.size} 條歷史消息")
            return@withContext messages
        } catch (e: Exception) {
            Log.e(TAG, "獲取用戶 $user1Id 和 $user2Id 之間的消息時出錯", e)
            return@withContext emptyList()
        }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        // ... (實現與之前版本相同) ...
        try {
            Log.d(TAG, "準備發送消息從 $senderId 到 $receiverId")
            val newMessage = Message( id = "", senderId = senderId, receiverId = receiverId, content = content )
            supabaseClient.postgrest[MESSAGES_TABLE].insert(newMessage)
            Log.i(TAG, "消息從 $senderId 到 $receiverId 發送成功")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "發送消息從 $senderId 到 $receiverId 時出錯", e)
            return@withContext false
        }
    }

    // --- 聊天列表相關方法 ---

    suspend fun getChatListItems(userId: String): List<ChatListItem> = withContext(Dispatchers.IO) {
        // ... (實現與之前版本相同，注意其效率問題) ...
        Log.d(TAG, "開始為用戶 $userId 獲取聊天列表項 (基礎實現)")
        val chatListItems = mutableListOf<ChatListItem>()
        try {
            // 1. 獲取對話夥伴 ID
            val sentMessages = supabaseClient.postgrest[MESSAGES_TABLE]
                .select(columns = Columns.list("receiver_id")) { filter { eq("sender_id", userId) } }
                .decodeList<Map<String, String>>()
            val receivedMessages = supabaseClient.postgrest[MESSAGES_TABLE]
                .select(columns = Columns.list("sender_id")) { filter { eq("receiver_id", userId) } }
                .decodeList<Map<String, String>>()
            val partnerIds = (sentMessages.mapNotNull { it["receiver_id"] } + receivedMessages.mapNotNull { it["sender_id"] })
                .filter { it != userId }.distinct()
            if (partnerIds.isEmpty()) return@withContext emptyList()
            Log.d(TAG, "找到 ${partnerIds.size} 個對話夥伴: $partnerIds")

            // 2. 批量獲取 Profile
            val partnerProfiles = profileRepository.getProfiles(partnerIds).associateBy { it.userId }
            Log.d(TAG, "成功獲取 ${partnerProfiles.size} 個對話夥伴的 Profile。")

            // 3. 並發獲取最後消息
            val deferredLastMessages = partnerIds.map { partnerId ->
                async(Dispatchers.IO) {
                    try {
                        supabaseClient.postgrest[MESSAGES_TABLE]
                            .select {
                                filter { or { and { eq("sender_id", userId); eq("receiver_id", partnerId) }
                                    and { eq("sender_id", partnerId); eq("receiver_id", userId) } } }
                                order("created_at", Order.DESCENDING)
                                limit(1)
                            }.decodeSingleOrNull<Message>()
                    } catch (e: Exception) { Log.e(TAG, "獲取用戶 $userId 和 $partnerId 的最後消息時出錯", e); null }
                }
            }
            val lastMessages = deferredLastMessages.awaitAll()

            // 4. 組合數據
            partnerIds.forEachIndexed { index, partnerId ->
                val profile = partnerProfiles[partnerId]
                val lastMessage = lastMessages[index]
                if (profile != null) {
                    val lastMessagePreview = lastMessage?.let { LastMessagePreview(content = it.content, timestamp = it.createdAt) }
                    chatListItems.add(ChatListItem(partnerProfile = profile, lastMessage = lastMessagePreview))
                } else { Log.w(TAG, "未找到夥伴 $partnerId 的 Profile，跳過此對話。") }
            }
            // 5. 排序
            chatListItems.sortByDescending { it.lastMessage?.timestamp }

        } catch (e: Exception) {
            Log.e(TAG, "獲取用戶 $userId 的聊天列表項時發生頂層錯誤", e)
            return@withContext emptyList()
        }
        Log.d(TAG, "完成為用戶 $userId 獲取聊天列表項，共 ${chatListItems.size} 項。")
        return@withContext chatListItems
    }


    // --- Realtime 相關方法 ---

    suspend fun createMessagesFlow(user1Id: String, user2Id: String): Flow<PostgresAction> {
        val channelId = generateChannelId(user1Id, user2Id)
        Log.d(TAG, "準備加入或創建 Realtime 頻道: $channelId")
        leaveChannel() // 確保清理舊頻道

        return callbackFlow {
            Log.d(TAG, "進入 createMessagesFlow 的 callbackFlow")
            try {
                chatChannel = supabaseClient.realtime.channel(channelId)

                val statusJob = repositoryScope.launch {
                    chatChannel?.status?.collect { status ->
                        Log.d(TAG, "頻道 $channelId 狀態變化: $status")
                        _realtimeStatus.value = mapRealtimeStatus(status)
                    }
                }

                val changeFlowJob = repositoryScope.launch {
                    val currentChannel = chatChannel
                    if (currentChannel == null) {
                        Log.e(TAG, "無法創建 postgresChangeFlow for $channelId: chatChannel is null")
                        _realtimeStatus.value = RealtimeState.Error("無法創建消息監聽器 (頻道為空)")
                        close(IllegalStateException("無法創建 postgresChangeFlow (頻道為空)"))
                        return@launch
                    }

                    val filterString = "or=(and(sender_id.eq.$user1Id,receiver_id.eq.$user2Id),and(sender_id.eq.$user2Id,receiver_id.eq.$user1Id))"
                    Log.d(TAG, "Realtime filter string: $filterString")

                    currentChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = MESSAGES_TABLE
//                        filter = filterString
                    }.catch { e ->
                        Log.e(TAG, "Realtime postgresChangeFlow 內部錯誤 for $channelId", e)
                        _realtimeStatus.value = RealtimeState.Error("監聽消息時出錯: ${e.message}")
                        close(e)
                    }.collect { action ->
                        Log.d(TAG, "頻道 $channelId 收到 PostgresAction: $action")
                        trySend(action)
                    }
                }

                Log.d(TAG, "正在訂閱頻道 $channelId...")
                _realtimeStatus.value = RealtimeState.Connecting
                chatChannel?.subscribe()

                awaitClose { // awaitClose lambda 開始
                    Log.d(TAG, "Flow for $channelId 被取消，正在清理資源...")
                    statusJob.cancel()       // 取消狀態監聽 Job
                    changeFlowJob.cancel()   // 取消消息監聽 Job

                    // --- 在協程中執行掛起的清理操作 ---
                    val cleanupJob = repositoryScope.launch { // 啟動一個新的協程
                        try {
                            leaveChannelBlocking() // 在協程中調用 suspend 函數
                            Log.d(TAG, "頻道 $channelId 異步清理完成。")
                        } catch (e: Exception) {
                            Log.e(TAG, "在 awaitClose 的協程中清理頻道 $channelId 時出錯", e)
                        }
                    }
                    // 注意：上面的 launch 是異步的，awaitClose 會立即返回。
                    // 如果需要確保清理完成後 awaitClose 才結束（雖然通常不需要），
                    // 可以使用 runBlocking(repositoryScope.coroutineContext) { ... }
                    // 但 runBlocking 可能會阻塞線程，需要謹慎使用。
                    // 對於清理操作，異步啟動通常足夠了。

                    Log.d(TAG, "awaitClose for $channelId 已觸發異步清理。")
                } // awaitClose lambda 結束

            } catch (e: Exception) {
                Log.e(TAG, "創建或訂閱頻道 $channelId 時出錯", e)
                _realtimeStatus.value = RealtimeState.Error("建立實時連接失敗: ${e.message}")
                close(e)
            }
        }
    }

    fun getRealtimeStatusFlow(): StateFlow<RealtimeState> {
        return _realtimeStatus.asStateFlow()
    }

    /**
     * 離開當前的 Realtime 頻道（公共入口，掛起函數）。
     */
    suspend fun leaveChannel() = withContext(Dispatchers.IO) {
        leaveChannelBlocking()
    }

    /**
     * 離開頻道的掛起版本（用於 awaitClose 或 onCleared）。
     */
    private suspend fun leaveChannelBlocking() { // <--- 添加 suspend 關鍵字
        val currentChannel = chatChannel
        if (currentChannel != null) {
            try {
                val channelId = currentChannel.topic
                Log.d(TAG, "正在取消訂閱並移除頻道 $channelId...")
                // unsubscribe 可能也是 suspend，可以直接 await 或 launch
                currentChannel.unsubscribe() // 假設 unsubscribe 是 suspend 或非阻塞請求

                // 調用 suspend 版本的 removeChannel
                supabaseClient.realtime.removeChannel(currentChannel)

                chatChannel = null
                _realtimeStatus.value = RealtimeState.Closed
                Log.d(TAG, "頻道 $channelId 已移除。")
            } catch (e: Exception) {
                Log.e(TAG, "離開頻道 ${currentChannel.topic} 時出錯", e)
                _realtimeStatus.value = RealtimeState.Error("離開頻道時出錯: ${e.message}")
            }
        } else {
            _realtimeStatus.value = RealtimeState.Closed
        }
    }

    /**
     * 生成唯一的頻道 ID。
     */
    private fun generateChannelId(user1Id: String, user2Id: String): String {
        val ids = listOf(user1Id, user2Id).sorted()
        return "chat-${ids[0]}-${ids[1]}"
    }


     /* 將 Supabase Realtime 狀態映射到自定義狀態。
     * 根據 RealtimeChannel.Status 的實際枚舉定義進行映射。
     */
    private fun mapRealtimeStatus(status: RealtimeChannel.Status): RealtimeState {
        Log.d(TAG, "Mapping Realtime Channel Status: $status")
        return when (status) {
            // 將枚舉常量映射到我們自定義的狀態
            RealtimeChannel.Status.UNSUBSCRIBED -> RealtimeState.Closed     // 未訂閱，視為關閉
            RealtimeChannel.Status.SUBSCRIBING -> RealtimeState.Connecting // 正在訂閱，視為連接中
            RealtimeChannel.Status.SUBSCRIBED -> RealtimeState.Open       // 已訂閱，視為打開
            RealtimeChannel.Status.UNSUBSCRIBING -> RealtimeState.Connecting // 正在取消訂閱，視為連接中/過渡
            // 因為是枚舉，覆蓋所有常量後，理論上不需要 else
        }
        // 注意：錯誤狀態 (Error) 需要通過其他方式監聽，例如 Flow 的 catch 操作符
    }

    /**
     * 清理 Repository Scope。
     */
    fun cleanup() {
        Log.d(TAG, "正在清理 ChatRepository 資源...")
        // 需要在協程中調用 suspend 函數
        repositoryScope.launch { leaveChannelBlocking() }
        repositoryScope.cancel()
    }

    // --- 如果包含 ContactRepository 的方法，請放在這裡 ---
    // ... (addContact, acceptFriendRequest etc.)
}
