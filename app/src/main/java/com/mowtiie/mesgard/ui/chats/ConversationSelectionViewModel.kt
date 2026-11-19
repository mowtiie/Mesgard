package com.mowtiie.mesgard.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
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

internal data class ConversationSelectionUiState(
    val selected: List<ConversationSummary> = emptyList(),
    val error: PreferenceError? = null,
    /** The conversations just archived, while Undo is offered. */
    val undoArchive: Set<String> = emptySet(),
    val deletion: DeletionState = DeletionState(),
) {
    val isActive: Boolean
        get() = selected.isNotEmpty()

    val selectedIds: Set<String>
        get() = selected.mapTo(mutableSetOf()) { it.id }

    val actions: SelectionActions
        get() = selectionActionsFor(selected)
}

/** Long pressing a conversation starts a selection; the toolbar then acts on every selected conversation at once. */
@HiltViewModel
internal class ConversationSelectionViewModel @Inject constructor(
    conversationRepository: ConversationRepository,
    private val preferences: ConversationPreferencesRepository,
) : ViewModel() {
    private val selectedIds = MutableStateFlow(emptySet<String>())
    private val error = MutableStateFlow<PreferenceError?>(null)
    private val undoArchive = MutableStateFlow(emptySet<String>())
    private val report = { result: PreferenceResult ->
        if (result is PreferenceResult.Failure) error.value = result.error
    }

    val deletion = ConversationDeletion(preferences, viewModelScope) { error.value = it }

    /** A conversation that leaves the list, for example when it is archived elsewhere, leaves the selection too. */
    val uiState: StateFlow<ConversationSelectionUiState> = combine(
        combine(conversationRepository.observeConversations(), conversationRepository.observeArchived()) {
                open,
                archived,
            ->
            open + archived
        },
        selectedIds,
        error,
        undoArchive,
        deletion.state,
    ) { conversations, ids, currentError, undo, deleting ->
        ConversationSelectionUiState(conversations.filter { it.id in ids }, currentError, undo, deleting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ConversationSelectionUiState(),
    )

    fun onToggled(conversationId: String) = selectedIds.update { ids ->
        if (conversationId in ids) ids - conversationId else ids + conversationId
    }

    fun onSelectAll(conversationIds: Collection<String>) {
        selectedIds.value = conversationIds.toSet()
    }

    fun onSelectionClosed() {
        selectedIds.value = emptySet()
    }

    fun onMuteClicked() = apply { ids, actions ->
        preferences.setMuted(ids, muted = actions.mute == MuteAction.MUTE)
    }

    fun onPinClicked() = apply { ids, actions ->
        preferences.setPinned(ids, pinned = actions.pin == PinAction.PIN)
    }

    fun onArchiveClicked() = apply { ids, actions ->
        val archive = actions.archive == ArchiveAction.ARCHIVE
        preferences.setArchived(ids, archive).also { result ->
            if (archive && result is PreferenceResult.Success) undoArchive.value = ids
        }
    }

    /** Archiving is undone when the snackbar action is taken, and forgotten when it goes away. */
    fun onUndoArchiveHandled(undo: Boolean) {
        val ids = undoArchive.value
        undoArchive.value = emptySet()
        if (undo && ids.isNotEmpty()) viewModelScope.launch { report(preferences.setArchived(ids, false)) }
    }

    fun onReadClicked() = apply { ids, actions ->
        if (actions.read == ReadAction.MARK_READ) preferences.markRead(ids) else preferences.markUnread(ids)
    }

    fun onErrorShown() {
        error.value = null
    }

    private fun apply(action: suspend (Set<String>, SelectionActions) -> PreferenceResult) {
        val state = uiState.value
        if (!state.isActive) return
        selectedIds.value = emptySet()
        viewModelScope.launch {
            report(action(state.selectedIds, state.actions))
        }
    }
}
