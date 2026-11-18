package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.SearchCursor
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.domain.model.SearchOutcome
import com.mowtiie.mesgard.domain.repository.ConversationSearchRepository
import com.mowtiie.mesgard.domain.repository.SEARCH_MIN_LENGTH
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DELAY_MS = 300L

internal data class ConversationSearchUiState(
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val hasFailed: Boolean = false,
    val jumpTo: String? = null,
) {
    val isLongEnough: Boolean
        get() = query.trim().length >= SEARCH_MIN_LENGTH

    val showNoResults: Boolean
        get() = isLongEnough && !isLoading && !hasFailed && hits.isEmpty()
}

/** Searches the messages of the conversation on the server, because the phone only holds the recent ones. */
@HiltViewModel
internal class ConversationSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConversationSearchRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(ConversationSearchUiState())
    val uiState: StateFlow<ConversationSearchUiState> = mutableUiState.asStateFlow()
    private var next: SearchCursor? = null
    private var search: Job? = null

    fun onQueryChanged(query: String) {
        search?.cancel()
        next = null
        mutableUiState.update { it.copy(query = query, hits = emptyList(), hasMore = false, hasFailed = false) }
        if (query.trim().length < SEARCH_MIN_LENGTH) {
            mutableUiState.update { it.copy(isLoading = false) }
            return
        }
        mutableUiState.update { it.copy(isLoading = true) }
        search = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            load(query.trim(), before = null)
        }
    }

    fun onLoadMore() {
        val cursor = next ?: return
        val state = mutableUiState.value
        if (state.isLoading) return
        mutableUiState.update { it.copy(isLoading = true) }
        search = viewModelScope.launch { load(state.query.trim(), cursor) }
    }

    fun onHitSelected(hit: SearchHit) = mutableUiState.update { it.copy(jumpTo = hit.messageId) }

    fun onJumpHandled() = mutableUiState.update { it.copy(jumpTo = null) }

    /** Takes the list to a message that is not a search result, such as a pinned one. */
    fun onJumpRequested(messageId: String) = mutableUiState.update { it.copy(jumpTo = messageId) }

    /** Leaving the search forgets it, but not a jump that is still on its way to the message. */
    fun onClosed() {
        search?.cancel()
        next = null
        mutableUiState.update { ConversationSearchUiState(jumpTo = it.jumpTo) }
    }

    private suspend fun load(query: String, before: SearchCursor?) {
        when (val outcome = repository.search(conversationId, query, before)) {
            is SearchOutcome.Loaded -> {
                next = outcome.page.next
                mutableUiState.update {
                    it.copy(
                        hits = if (before == null) outcome.page.hits else it.hits + outcome.page.hits,
                        hasMore = outcome.page.next != null,
                        isLoading = false,
                    )
                }
            }
            is SearchOutcome.Failure -> mutableUiState.update { it.copy(isLoading = false, hasFailed = true) }
        }
    }
}
