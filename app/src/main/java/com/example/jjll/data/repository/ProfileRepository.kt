package com.example.jjll.data.repository

import com.example.jjll.data.model.Profile

interface ProfileRepository {
    /**
     * Fetches the profile for the currently logged-in user.
     * @return The user's Profile object, or null if not found or not logged in.
     */
    suspend fun getCurrentUserProfile(): Result<Profile?>

    /**
     * Fetches a list of all user profiles.
     * In a real app, this should be paginated or changed to a search function.
     * @return A Result containing a list of all Profile objects.
     */
    suspend fun getAllUsers(): Result<List<Profile>>
}