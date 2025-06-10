package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import com.example.jjll.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import jakarta.inject.Inject


// ... imports ...
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    // State for search query, search results, loading, error
    // fun searchUsers(query: String)
    // fun sendRequest(receiverId: String, message: String)
}