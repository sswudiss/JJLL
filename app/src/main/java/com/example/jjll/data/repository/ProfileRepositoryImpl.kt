package com.example.jjll.data.repository

import android.util.Log
import com.example.jjll.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    private val profilesTable = "profiles"

    /**
     * Fetches the profile for the currently logged-in user.
     * Uses the user's authentication ID to find the corresponding profile.
     */
    override suspend fun getCurrentUserProfile(): Result<Profile?> {
        Log.d("ProfileRepository", "Fetching current user profile...")
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w("ProfileRepository", "Current user is not logged in. Cannot fetch profile.")
                return Result.success(null) // Not an error, just no user logged in.
            }

            Log.d("ProfileRepository", "Current User ID: $currentUserId")
            val profile = supabaseClient.postgrest[profilesTable]
                .select {
                    // Filter the profiles table to find the row where the
                    // 'user_id' column matches the current user's auth ID.
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<Profile>() // Expecting zero or one result.

            Log.d("ProfileRepository", "Fetched current user profile: ${profile?.username}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error fetching current user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a list of all user profiles from the database.
     * This implementation fetches all users and sorts them by username.
     */
    override suspend fun getAllUsers(): Result<List<Profile>> {
        Log.d("ProfileRepository", "Fetching all users...")
        return try {
            val profiles = supabaseClient.postgrest[profilesTable]
                .select(columns = Columns.ALL) { // Explicitly select all columns
                    order("username", Order.ASCENDING) // Sort by username alphabetically
                }
                .decodeList<Profile>() // Expecting a list of results.

            Log.d("ProfileRepository", "Fetched ${profiles.size} users.")
            Result.success(profiles)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error fetching all users: ${e.message}", e)
            Result.failure(e)
        }
    }

}