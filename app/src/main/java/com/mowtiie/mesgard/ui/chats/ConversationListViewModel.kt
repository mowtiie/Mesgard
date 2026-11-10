package com.mowtiie.mesgard.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class ConversationListViewModel @Inject constructor(private val repository: ConversationRepository) :
    ViewModel() {
    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<ConversationListUiState> = combine(
        repository.observeConversations(),
        refreshState,
    ) { conversations, refresh ->
        ConversationListUiState(
            conversations = conversations,
            isLoaded = true,
            isRefreshing = refresh.isRefreshing,
            refreshError = refresh.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ConversationListUiState(),
    )

    init {
        onRefresh()
    }

    fun onRefresh() {
        if (refreshState.value.isRefreshing) return
        refreshState.update { RefreshState(isRefreshing = true) }
        viewModelScope.launch {
            val result = repository.refresh()
            refreshState.update { RefreshState(error = (result as? RefreshResult.Failure)?.error) }
        }
    }

    fun onRefreshErrorShown() {
        refreshState.update { it.copy(error = null) }
    }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: RefreshError? = null)
}
