package com.example.jjll.data.repository

import com.example.jjll.data.model.FriendRequest
import com.example.jjll.data.model.Friendship
import com.example.jjll.data.model.Profile

interface FriendRepository {
    suspend fun searchUsers(query: String, currentUserId: String, limit: Int = 20): Result<List<Profile>>
    suspend fun sendFriendRequest(receiverId: String, message: String): Result<Unit>
    suspend fun getReceivedFriendRequests(userId: String): Result<List<FriendRequest>>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun declineFriendRequest(requestId: String): Result<Unit>
    suspend fun getFriends(userId: String): Result<List<Friendship>>
}