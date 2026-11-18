package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.repository.MAX_MENTIONS_PER_MESSAGE
import com.mowtiie.mesgard.domain.repository.MentionRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SEARCH_DELAY_MS = 200L

/** Who can be chosen after an @ in the draft. */
internal data class MentionUiState(val suggestions: List<MentionCandidate> = emptyList())

/** Suggests people of the conversation while an @ is being typed and remembers who was chosen for the next message. */
@HiltViewModel
internal class MentionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MentionRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(MentionUiState())
    val uiState: StateFlow<MentionUiState> = mutableUiState.asStateFlow()
    private val picks = linkedMapOf<String, String>()
    private var search: Job? = null

    fun onDraftChanged(draft: String) {
        search?.cancel()
        val query = mentionQuery(draft)
        if (query == null || picks.size >= MAX_MENTIONS_PER_MESSAGE) {
            mutableUiState.value = MentionUiState()
            return
        }
        search = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            mutableUiState.value = MentionUiState(repository.search(conversationId, query))
        }
    }

    /** Returns the draft with the chosen name in it. */
    fun onSelected(candidate: MentionCandidate, draft: String): String {
        search?.cancel()
        picks[candidate.userId] = candidate.displayName
        mutableUiState.value = MentionUiState()
        return insertMention(draft, candidate.displayName)
    }

    fun mentionedIds(draft: String): List<String> = mentionedIdsIn(draft, picks)

    fun onSent() {
        search?.cancel()
        picks.clear()
        mutableUiState.value = MentionUiState()
    }
}
