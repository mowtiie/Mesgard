package com.mowtiie.mesgard.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.TypingRepository
import com.mowtiie.mesgard.ui.common.matchesQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
private const val TYPING_WATCH_LIMIT = 15

@HiltViewModel
internal class ConversationListViewModel @Inject constructor(
    private val repository: ConversationRepository,
    typingRepository: TypingRepository,
) : ViewModel() {
    private val refreshState = MutableStateFlow(RefreshState())
    private val query = MutableStateFlow("")

    /** Only the conversations at the top are watched, because each one is a Realtime channel. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val typing: Flow<Map<String, List<String>>> = repository.observeConversations()
        .map { conversations -> conversations.take(TYPING_WATCH_LIMIT).mapTo(mutableSetOf()) { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids -> typingRepository.observeTypingIn(ids) }

    val uiState: StateFlow<ConversationListUiState> = combine(
        repository.observeConversations(),
        repository.observeArchived(),
        refreshState,
        query,
        typing,
    ) { conversations, archived, refresh, currentQuery, typingNow ->
        ConversationListUiState(
            conversations = conversations.filter { it.matches(currentQuery) },
            totalConversations = conversations.size,
            archivedCount = archived.size,
            typing = typingNow,
            query = currentQuery,
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

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun onRefreshErrorShown() {
        refreshState.update { it.copy(error = null) }
    }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: RefreshError? = null)
}

private fun ConversationSummary.matches(query: String) =
    query.isBlank() || matchesQuery(title.orEmpty(), query) || matchesQuery(lastMessagePreview.orEmpty(), query)
