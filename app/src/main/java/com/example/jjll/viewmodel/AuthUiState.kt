package com.example.jjll.viewmodel

// 修正: 引入 auth-kt 下的 UserSession
import io.github.jan.supabase.auth.user.UserSession

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val session: UserSession? = null // UserSession 來自 auth-kt
)