// ChatRepository.kt (接口)
package com.example.jjll.data.repository

import com.example.jjll.data.model.Chat
import com.example.jjll.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * Finds an existing chat with the other user, or creates a new one.
     * @return A Result containing the chat_id string.
     */
    suspend fun findOrCreateChatWithUser(otherUserId: String): Result<String>

    /**
     * Gets the list of chats for the current user.
     * @return A Result containing a list of Chat objects.
     */
    suspend fun getChatList(): Result<List<Chat>>

    /**
     * Gets all messages for a given chat ID.
     * @return A Result containing a list of ChatMessage objects.
     */
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<ChatMessage>>

    /**
     * Sends a new message to a chat.
     * @return A Result indicating success or failure.
     */
    suspend fun sendMessage(chatId: String, content: String): Result<Unit>

    /**
     * Subscribes to new messages for a given chat ID.
     * @return A Flow that emits new ChatMessage objects.
     */
    fun getNewMessagesFlow(chatId: String): Flow<ChatMessage>
}