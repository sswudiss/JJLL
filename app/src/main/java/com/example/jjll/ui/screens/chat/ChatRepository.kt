package com.example.jjll.ui.screens.chat

import android.util.Log
import com.example.jjll.common.RepoResult
import com.example.jjll.data.Message
import com.example.jjll.data.NewMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// --- 常量定義 ---
private const val TAG = "ChatRepository"
private const val MESSAGES_TABLE = "messages"

@Singleton
class ChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient // 注入完整的 Client
) {
    // --- 屬性 Getter ---
    private val auth get() = supabaseClient.auth // 使用正確的 auth 擴展
    private val postgrest get() = supabaseClient.postgrest // 使用正確的 postgrest 擴展
    private val realtime get() = supabaseClient.realtime // 使用正確的 realtime 擴展

    // --- Coroutine Scope ---
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- 方法實現 ---

    /**
     * 發送一條新消息
     */
    suspend fun sendMessage(receiverId: String, content: String): RepoResult<Boolean> {
        return withContext(Dispatchers.IO) {
            val currentUserId = auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.e(TAG, "SendMessage failed: User not logged in.")
                return@withContext RepoResult.Error("用戶未登錄")
            }
            if (content.isBlank()) {
                Log.w(TAG, "SendMessage failed: Content is blank.")
                return@withContext RepoResult.Error("消息內容不能為空")
            }
            if (currentUserId == receiverId) {
                Log.w(TAG, "SendMessage failed: Cannot send message to self.")
                return@withContext RepoResult.Error("不能給自己發消息")
            }

            val newMessage = NewMessage(
                sender_id = currentUserId,
                receiver_id = receiverId,
                content = content.trim()
            )
            Log.d(TAG, "Sending message from $currentUserId to $receiverId: ${newMessage.content}")

            try {
                postgrest[MESSAGES_TABLE].insert(newMessage) // 不需要 <NewMessage> 泛型，也不需要 returning
                Log.i(TAG, "Message sent successfully.")
                RepoResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                RepoResult.Error("發送消息失敗: ${e.message}", e)
            }
        }
    }

    /**
     * 獲取與特定用戶的聊天記錄 (按時間排序)
     */
    suspend fun getMessagesWithUser(otherUserId: String, limit: Long = 50): RepoResult<List<Message>> {
        return withContext(Dispatchers.IO) {
            val currentUserId =
                auth.currentUserOrNull()?.id ?: return@withContext RepoResult.Error("用戶未登錄")
            Log.d(TAG, "Fetching messages between $currentUserId and $otherUserId (limit: $limit)")

            try {
                val messages = postgrest[MESSAGES_TABLE]
                    .select { // 使用 select 配置 lambda
                        filter { // 在 filter 內部構建條件
                            or {
                                and {
                                    eq("sender_id", currentUserId)
                                    eq("receiver_id", otherUserId)
                                }
                                and {
                                    eq("sender_id", otherUserId)
                                    eq("receiver_id", currentUserId)
                                }
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(limit)
                    }
                    .decodeList<Message>() // 解碼為 Message 列表

                val orderedMessages = messages.reversed() // 反轉列表，UI 顯示舊消息在頂部

                Log.i(
                    TAG,
                    "Fetched ${orderedMessages.size} messages between $currentUserId and $otherUserId"
                )
                RepoResult.Success(orderedMessages)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching messages between $currentUserId and $otherUserId", e)
                RepoResult.Error("獲取消息失敗: ${e.message}", e)
            }
        }
    }

    /**
     * 使用 Supabase Realtime 監聽與特定用戶的新消息
     */
    fun getNewMessagesFlow(otherUserId: String): Flow<Message> {
        val currentUserId = auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            Log.e(TAG, "Cannot listen for new messages, user not logged in.")
            return emptyFlow()
        }

        Log.d(TAG, "Starting to listen for new messages between $currentUserId and $otherUserId")

        val channel: RealtimeChannel = supabaseClient.channel("messages_${currentUserId}_$otherUserId")

        val insertFlow: Flow<Message> = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = MESSAGES_TABLE
            // 使用 filter 方法，並確保 FilterOperator 導入正確
            try {
                filter("receiver_id", FilterOperator.EQ, currentUserId)
            } catch (e: Exception) {
                Log.e(TAG, "Error applying filter 'receiver_id=eq.$currentUserId'. Trying string filter.", e)
                // 如果上面報錯，嘗試字符串形式 (備選方案)
                // filter = "receiver_id=eq.$currentUserId"
            }
        }
            .mapNotNull { action ->
                try {
                    // 使用 decodeRecordOrNull 進行解碼
                    action.decodeRecordOrNull<Message>() // 確保導入 decodeRecordOrNull
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode incoming message record. Record: ${action.record}", e) // 使用 action.record
                    null
                }
            }
            .filter { message -> // 客戶端過濾
                message.sender_id == otherUserId
            }

        Log.i(TAG,"Realtime flow created for messages between $currentUserId and $otherUserId")
        return insertFlow
            .onStart {
                if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED  && channel.status.value != RealtimeChannel.Status.SUBSCRIBING) {
                    // 使用 channel.topic 獲取標識符
                    Log.d(TAG, "Subscribing to channel on flow start: ${channel.topic}")
                    try { channel.subscribe(blockUntilSubscribed = true) }
                    catch (e: Exception) { Log.e(TAG, "Error subscribing to channel ${channel.topic}", e) }
                } else {
                    Log.d(TAG, "Channel ${channel.topic} already ${channel.status.value}")
                }
            }
            .onCompletion {
                Log.d(TAG, "Realtime flow completed. Unsubscribing channel: ${channel.topic}")
                repositoryScope.launch {
                    try { realtime.removeChannel(channel) }
                    catch(e: Exception) { Log.e(TAG,"Error removing channel ${channel.topic}", e) }
                }
            }
            .catch { e -> Log.e(TAG, "Error in Realtime flow", e) }
            .flowOn(Dispatchers.IO)
    }


    /**
     * 連接 Realtime Channel (由外部調用)
     */
    suspend fun connectRealtimeChannel(otherUserId: String) {
        val currentUserId = auth.currentUserOrNull()?.id ?: return
        val topic = "messages_${currentUserId}_$otherUserId" // 使用 topic
        val channel = realtime.channel(topic)
        if (channel.status.value != RealtimeChannel.Status.UNSUBSCRIBED && channel.status.value != RealtimeChannel.Status.SUBSCRIBING) {
            Log.d(TAG, "Subscribing to Realtime channel: $topic")
            try {
                channel.subscribe(blockUntilSubscribed = true)
                Log.i(TAG, "Successfully subscribed to channel: $topic")
            } catch (e: Exception) { Log.e(TAG,"Error subscribing to channel $topic", e) }
        } else {
            Log.d(TAG, "Channel $topic already ${channel.status.value}")
        }
    }

    /**
     * 斷開 Realtime Channel (由外部調用)
     */
    suspend fun disconnectRealtimeChannel(otherUserId: String) {
        val currentUserId = auth.currentUserOrNull()?.id ?: return
        val topic = "messages_${currentUserId}_$otherUserId" // 使用 topic
        Log.d(TAG, "Unsubscribing from Realtime channel: $topic")
        try {
            val channel = realtime.channel(topic) // 先獲取再移除
            realtime.removeChannel(channel)
            Log.i(TAG,"Successfully removed channel: $topic")
        } catch (e: Exception) { Log.e(TAG,"Error removing channel $topic", e) }
    }

} // Class ChatRepository 結束