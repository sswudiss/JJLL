package com.example.jjll.data.repository

import android.util.Log
import com.example.jjll.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    private val messagesTable = "messages"

    override suspend fun findOrCreateChatWithUser(otherUserId: String): Result<String> {
        return try {
            // 1. 獲取當前登錄用戶的 ID
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(IllegalStateException("User not logged in"))  // 如果未登錄，則返回失敗

            // 2. 調用 RPC 函數，並傳遞兩個參數
            val result = supabaseClient.postgrest.rpc(
                function = "find_or_create_chat",
                parameters = mapOf(
                    "user1_id_input" to currentUserId,
                    "user2_id_input" to otherUserId
                )
            )

            // 3. 從結果中解碼出 chat ID
            // 修正點：使用 result 對象上的 decodeAs<T>() 方法
            // 我們的 RPC 返回一個單一的 uuid 值，它被 PostgREST 當作一個 JSON 字符串返回。
            // decodeAs<String>() 應該能夠將這個 JSON 字符串 (例如 "your-uuid") 解碼為一個 Kotlin String。
            val chatId = result.decodeAs<String>()

            Log.d("ChatRepository", "Successfully found or created chat with ID: $chatId")
            Result.success(chatId)

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error finding or creating chat: ${e.message}", e)
            Result.failure(e)
        }
    }


    override suspend fun getChatList(): Result<List<Chat>> {
        return try {
            val currentUserId =
                supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(
                    IllegalStateException("User not logged in")
                )

            // 用於解碼 RPC 函數返回的數據
            @Serializable
            data class RpcChatResult(
                @SerialName("chat_id") val chatId: String,
                @SerialName("last_message_content") val lastMessageContent: String?,
                @SerialName("last_message_created_at") val lastMessageCreatedAt: String?,
                @SerialName("other_participant_id") val otherParticipantId: String,
                @SerialName("other_participant_username") val otherParticipantUsername: String?,
                @SerialName("other_participant_avatar_url") val otherParticipantAvatarUrl: String?
            )

            // 調用 RPC 函數
            val rpcResults = supabaseClient.postgrest.rpc(
                function = "get_user_chats",
                parameters = mapOf("p_user_id" to currentUserId)
            ).decodeList<RpcChatResult>()

            // 將 RPC 結果映射到我們的 App 數據模型
            val chatList = rpcResults.map { result ->
                Chat(
                    id = result.chatId,
                    createdAt = "", // RPC 結果中未包含，如果需要可以添加
                    otherParticipantProfile = Profile(
                        userId = result.otherParticipantId,
                        username = result.otherParticipantUsername,
                        avatarUrl = result.otherParticipantAvatarUrl,
                        createdAt = "" // 同上
                    ),
                    lastMessage = if (result.lastMessageContent != null && result.lastMessageCreatedAt != null) {
                        ChatMessage(
                            id = "", // 不重要
                            chatId = result.chatId,
                            senderId = "", // 不重要
                            content = result.lastMessageContent,
                            createdAt = result.lastMessageCreatedAt
                        )
                    } else {
                        null
                    }
                )
            }
            Result.success(chatList)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat list: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getMessages(chatId: String, limit: Int): Result<List<ChatMessage>> {
        Log.d("ChatRepository", "Fetching messages for chatId: $chatId")
        return try {
            val messages = supabaseClient.postgrest[messagesTable].select {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<ChatMessage>()
            // 因為我們是倒序獲取的，所以在返回給 UI 前需要反轉列表，
            // 這樣消息在界面上就會以正確的時間順序（舊->新）顯示。
            val sortedMessages = messages.reversed()

            Log.d("ChatRepository", "Fetched ${sortedMessages.size} messages.")
            Result.success(sortedMessages)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching messages: ${e.message}", e)
            Result.failure(e)
        }
    }


    override suspend fun sendMessage(chatId: String, content: String): Result<Unit> {
        return try {
            val senderId = supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(
                IllegalStateException("User not logged in")
            )

            // 創建要插入的數據 Map。我們不需要提供 id 或 created_at，數據庫會自動生成。
            val messageData = mapOf(
                "chat_id" to chatId,
                "sender_id" to senderId,
                "content" to content
            )

            // 執行插入操作
            supabaseClient.postgrest[messagesTable].insert(value = messageData)

            // 如果沒有拋出異常，則認為成功
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getNewMessagesFlow(chatId: String): Flow<ChatMessage> {
        // 1. 為這個聊天創建一個唯一的 channel 名稱
        val channel = supabaseClient.channel("chat-$chatId")

        // 2. 使用 postgresChangeFlow 來監聽數據庫變更
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            // 'this' 的上下文是 PostgresChangeFilter
            table = messagesTable

            // 只監聽與當前 chatId 匹配的行的變更
            filter("chat_id", FilterOperator.EQ, chatId)
        }.map { insertAction -> // 'it' 的類型是 PostgresAction.Insert
            // 3. 當收到一個 Insert 事件時，將其 record 解碼為我們的 ChatMessage 對象
            insertAction.decodeRecord<ChatMessage>()
        }
    }
}