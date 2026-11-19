package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.MessageReader
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.model.ReadersResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The "Seen by" list of one message, while it loads, when it arrived and when it could not be loaded. */
internal sealed interface ReadersState {
    data object Loading : ReadersState

    data class Loaded(val readers: List<MessageReader>) : ReadersState

    data object Failed : ReadersState
}

internal data class ConversationActionsUiState(
    val messageToDelete: String? = null,
    val readers: ReadersState? = null,
    val error: ConversationError? = null,
)

/**
 * What the user can do beyond reading and writing: delete a message they sent, after asking, and mute the chat,
 * which shows at once and is taken back when the server refuses.
 */
@HiltViewModel
internal class ConversationActionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageActions: MessageActionsRepository,
    private val conversationRepository: ConversationRepository,
    private val preferences: ConversationPreferencesRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(ConversationActionsUiState())
    val uiState: StateFlow<ConversationActionsUiState> = mutableUiState.asStateFlow()

    fun onDeleteRequested(messageId: String) = mutableUiState.update { it.copy(messageToDelete = messageId) }

    fun onDeleteDismissed() = mutableUiState.update { it.copy(messageToDelete = null) }

    fun onDeleteConfirmed() {
        val messageId = mutableUiState.value.messageToDelete ?: return
        mutableUiState.update { it.copy(messageToDelete = null) }
        viewModelScope.launch { report(messageActions.delete(messageId)) }
    }

    fun onInfoRequested(messageId: String) {
        mutableUiState.update { it.copy(readers = ReadersState.Loading) }
        viewModelScope.launch {
            val state = when (val result = messageActions.readers(messageId)) {
                is ReadersResult.Loaded -> ReadersState.Loaded(result.readers)
                is ReadersResult.Failure -> ReadersState.Failed
            }
            mutableUiState.update { current -> if (current.readers == null) current else current.copy(readers = state) }
        }
    }

    fun onInfoDismissed() = mutableUiState.update { it.copy(readers = null) }

    fun onMuteChanged(muted: Boolean) {
        viewModelScope.launch {
            val result = preferences.setMuted(setOf(conversationId), muted)
            if (result is PreferenceResult.Failure) mutableUiState.update { it.copy(error = result.error.toError()) }
        }
    }

    /** Opening a conversation that was marked as unread reads it. */
    fun onOpened() {
        viewModelScope.launch {
            val conversation = conversationRepository.observeConversation(conversationId).first()
            if (conversation?.isMarkedUnread == true) preferences.markRead(setOf(conversationId))
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    private fun report(result: RefreshResult) {
        if (result is RefreshResult.Failure) mutableUiState.update { it.copy(error = result.error.toError()) }
    }
}

private fun PreferenceError.toError() = when (this) {
    PreferenceError.NETWORK_UNAVAILABLE -> ConversationError.NETWORK_UNAVAILABLE
    else -> ConversationError.UNKNOWN
}
