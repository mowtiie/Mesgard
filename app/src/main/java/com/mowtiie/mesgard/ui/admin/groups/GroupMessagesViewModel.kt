package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.AccessedMessage
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import com.mowtiie.mesgard.ui.navigation.GroupMessagesRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

internal data class GroupMessagesUiState(
    val messages: List<AccessedMessage> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val error: AdminError? = null,
)

/** The messages of a group opened with break-glass access, newest first, read only. */
@HiltViewModel
internal class GroupMessagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GroupAccessRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupMessagesRoute>().groupId
    private val mutableUiState = MutableStateFlow(GroupMessagesUiState())
    val uiState: StateFlow<GroupMessagesUiState> = mutableUiState.asStateFlow()

    init {
        loadMore()
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun loadMore() {
        val state = mutableUiState.value
        if (state.messages.isNotEmpty() && (!state.hasMore || state.isLoading)) return
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.messages(groupId, state.messages.lastOrNull(), PAGE_SIZE)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        messages = it.messages + result.value,
                        isLoading = false,
                        hasMore = result.value.size == PAGE_SIZE,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}
