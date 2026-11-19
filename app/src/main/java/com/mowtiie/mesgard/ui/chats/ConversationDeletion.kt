package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [pending] waits for the person to confirm; [undo] is what was just deleted, while Undo is offered. */
internal data class DeletionState(val pending: Set<String> = emptySet(), val undo: Set<String> = emptySet())

/** Delete for me asks first, because it clears the history on this account, and offers Undo afterwards. */
internal class ConversationDeletion(
    private val preferences: ConversationPreferencesRepository,
    private val scope: CoroutineScope,
    private val onFailure: (PreferenceError) -> Unit,
) {
    private val mutableState = MutableStateFlow(DeletionState())
    val state: StateFlow<DeletionState> = mutableState.asStateFlow()

    fun onRequested(conversationIds: Set<String>) {
        if (conversationIds.isNotEmpty()) mutableState.value = DeletionState(pending = conversationIds)
    }

    fun onDismissed() {
        mutableState.value = mutableState.value.copy(pending = emptySet())
    }

    fun onConfirmed() {
        val ids = mutableState.value.pending
        if (ids.isEmpty()) return
        mutableState.value = DeletionState()
        scope.launch {
            when (val result = preferences.deleteForMe(ids)) {
                PreferenceResult.Success -> mutableState.value = DeletionState(undo = ids)
                is PreferenceResult.Failure -> onFailure(result.error)
            }
        }
    }

    fun onUndoHandled(undo: Boolean) {
        val ids = mutableState.value.undo
        mutableState.value = DeletionState()
        if (!undo || ids.isEmpty()) return
        scope.launch {
            val result = preferences.undoDeleteForMe(ids)
            if (result is PreferenceResult.Failure) onFailure(result.error)
        }
    }
}
