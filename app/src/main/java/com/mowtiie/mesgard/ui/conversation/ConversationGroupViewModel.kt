package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ConversationGroupUiState(
    val groupId: String? = null,
    val isFailed: Boolean = false,
    /** When an administrator last opened the messages of this group, if they did lately. */
    val lastOpenedAtEpochMillis: Long? = null,
)

/** Finds the group behind a group conversation, so its screen can be opened from the conversation. */
@HiltViewModel
internal class ConversationGroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationGroupRepository: ConversationGroupRepository,
    private val groupAccessRepository: GroupAccessRepository,
) : ViewModel() {
    private var hasLookedForOpenings = false
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(ConversationGroupUiState())
    val uiState: StateFlow<ConversationGroupUiState> = mutableUiState.asStateFlow()

    /** Members are told when an administrator opened their group, so the conversation asks once it knows it is one. */
    fun onGroupConversationShown() {
        if (hasLookedForOpenings) return
        hasLookedForOpenings = true
        viewModelScope.launch {
            val groupId = (conversationGroupRepository.groupOf(conversationId) as? AdminResult.Success)?.value
                ?: return@launch
            val openings = (groupAccessRepository.openings(groupId) as? AdminResult.Success)?.value
            mutableUiState.update { it.copy(lastOpenedAtEpochMillis = openings?.maxOrNull()) }
        }
    }

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
