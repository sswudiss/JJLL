package com.example.jjll.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jjll.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // ViewModel 初始化時自動加載聊天列表
        loadChatList()
    }

    /**
     * Loads the list of chats from the repository.
     * Can be called for initial load and for manual refresh.
     */
    fun loadChatList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = chatRepository.getChatList()

            result.fold(
                onSuccess = { chatList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chats = chatList
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "無法加載聊天列表: ${throwable.message}"
                        )
                    }
                }
            )
        }
    }
}