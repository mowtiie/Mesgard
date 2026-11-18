package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The message being edited, with the text as it was and as it is now. [messageId] is null when nothing is edited. */
internal data class MessageEditUiState(
    val messageId: String? = null,
    val originalText: String = "",
    val text: String = "",
    val isSaving: Boolean = false,
    val error: ConversationError? = null,
) {
    val isEditing: Boolean
        get() = messageId != null

    val canSave: Boolean
        get() = isEditing &&
            !isSaving &&
            text.isNotBlank() &&
            text.trim() != originalText &&
            text.trim().length <= MAX_MESSAGE_LENGTH
}

/** Editing has its own text, so the draft of the conversation stays as it was while a message is changed. */
@HiltViewModel
internal class MessageEditViewModel @Inject constructor(private val repository: MessageActionsRepository) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(MessageEditUiState())
    val uiState: StateFlow<MessageEditUiState> = mutableUiState.asStateFlow()

    fun onEditRequested(message: Message) = mutableUiState.update {
        MessageEditUiState(messageId = message.id, originalText = message.body, text = message.body)
    }

    fun onTextChanged(text: String) = mutableUiState.update { it.copy(text = text) }

    fun onEditCancelled() = mutableUiState.update { MessageEditUiState() }

    fun onSaveClicked() {
        val state = mutableUiState.value
        val messageId = state.messageId ?: return
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = repository.edit(messageId, state.text)) {
                RefreshResult.Success -> mutableUiState.update { MessageEditUiState() }
                is RefreshResult.Failure ->
                    mutableUiState.update { it.copy(isSaving = false, error = result.error.toError()) }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
