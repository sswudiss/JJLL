package com.example.jjll.data

sealed class RealtimeState {
    object Connecting : RealtimeState()
    object Open : RealtimeState()
    object Closed : RealtimeState()
    data class Error(val message: String?) : RealtimeState()
}