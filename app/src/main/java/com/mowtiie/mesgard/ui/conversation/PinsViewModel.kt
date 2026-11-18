package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.PinsOutcome
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MAX_PINNED_MESSAGES
import com.mowtiie.mesgard.domain.repository.PinRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PinsUiState(
    val pins: List<PinnedMessage> = emptyList(),
    val canPin: Boolean = false,
    val error: ConversationError? = null,
) {
    val pinnedIds: Set<String>
        get() = pins.mapTo(mutableSetOf()) { it.messageId }
}

/** The pinned messages of the conversation and, for administrators and staff, pinning and unpinning them. */
@HiltViewModel
internal class PinsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PinRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(PinsUiState())
    val uiState: StateFlow<PinsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCanPin(conversationId).collect { canPin ->
                mutableUiState.update { it.copy(canPin = canPin) }
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            // A phone without a connection keeps the pins it already shows.
            val outcome = repository.fetch(conversationId)
            if (outcome is PinsOutcome.Loaded) mutableUiState.update { it.copy(pins = outcome.pins) }
        }
    }

    fun onPin(messageId: String) {
        if (mutableUiState.value.pins.size >= MAX_PINNED_MESSAGES) {
            mutableUiState.update { it.copy(error = ConversationError.TOO_MANY_PINS) }
            return
        }
        change { repository.pin(conversationId, messageId) }
    }

    fun onUnpin(messageId: String) = change { repository.unpin(messageId) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    private fun change(action: suspend () -> RefreshResult) {
        viewModelScope.launch {
            when (val result = action()) {
                RefreshResult.Success -> onRefresh()
                is RefreshResult.Failure -> mutableUiState.update { it.copy(error = result.error.toError()) }
            }
        }
    }
}
