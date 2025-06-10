package com.example.jjll.data.repository

import android.util.Log
import com.example.jjll.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
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
            val currentUserId =
                supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(
                    IllegalStateException("User not logged in")
                )

            // 調用我們在 Supabase 中創建的 RPC 函數
            val chatId = supabaseClient.postgrest.rpc(
                function = "find_or_create_chat",
                parameters = mapOf(
                    "user1_id_input" to currentUserId,
                    "user2_id_input" to otherUserId
                )
            ).decodeSingle<String>() // 函數返回一個 uuid 字符串

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
        return try {
            val result = supabaseClient.postgrest[messagesTable].select {
                filter { eq("chat_id", chatId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<ChatMessage>()
            // 因為我們是倒序獲取的，所以在使用前反轉列表以保持時間順序
            Result.success(result.reversed())
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting messages: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(chatId: String, content: String): Result<Unit> {
        return try {
            val senderId = supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(
                IllegalStateException("User not logged in")
            )
            supabaseClient.postgrest[messagesTable].insert(
                value = mapOf(
                    "chat_id" to chatId,
                    "sender_id" to senderId,
                    "content" to content
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getNewMessagesFlow(chatId: String): Flow<ChatMessage> {
        val channel = supabaseClient.channel("chat-$chatId")
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = messagesTable
            filter("chat_id", FilterOperator.EQ, chatId)
        }.map { insertAction -> // 'it' 的類型是 PostgresAction.Insert
            // 修正點：使用庫提供的 decodeRecord() 擴展函數
            // 它會自動將 insertAction.record (一個 JsonObject) 解碼為 ChatMessage
            insertAction.decodeRecord<ChatMessage>()
        }
    }
}