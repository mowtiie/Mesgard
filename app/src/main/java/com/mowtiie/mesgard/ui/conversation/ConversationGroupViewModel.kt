package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ConversationGroupUiState(val groupId: String? = null, val isFailed: Boolean = false)

/** Finds the group behind a group conversation, so its screen can be opened from the conversation. */
@HiltViewModel
internal class ConversationGroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationGroupRepository: ConversationGroupRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(ConversationGroupUiState())
    val uiState: StateFlow<ConversationGroupUiState> = mutableUiState.asStateFlow()

    fun onGroupInfoRequested() {
        viewModelScope.launch {
            when (val result = conversationGroupRepository.groupOf(conversationId)) {
                is AdminResult.Success -> mutableUiState.update { it.copy(groupId = result.value) }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isFailed = true) }
            }
        }
    }

    fun onGroupOpened() = mutableUiState.update { it.copy(groupId = null) }

    fun onFailureShown() = mutableUiState.update { it.copy(isFailed = false) }
}
