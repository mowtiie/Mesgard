package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MessageActionsUiState(val messageToDelete: String? = null, val error: ConversationError? = null)

/** What the user can do with a message they sent: for now deleting it, after asking. */
@HiltViewModel
internal class MessageActionsViewModel @Inject constructor(private val repository: MessageActionsRepository) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(MessageActionsUiState())
    val uiState: StateFlow<MessageActionsUiState> = mutableUiState.asStateFlow()

    fun onDeleteRequested(messageId: String) = mutableUiState.update { it.copy(messageToDelete = messageId) }

    fun onDeleteDismissed() = mutableUiState.update { it.copy(messageToDelete = null) }

    fun onDeleteConfirmed() {
        val messageId = mutableUiState.value.messageToDelete ?: return
        mutableUiState.update { it.copy(messageToDelete = null) }
        viewModelScope.launch {
            val result = repository.delete(messageId)
            if (result is RefreshResult.Failure) mutableUiState.update { it.copy(error = result.error.toError()) }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
