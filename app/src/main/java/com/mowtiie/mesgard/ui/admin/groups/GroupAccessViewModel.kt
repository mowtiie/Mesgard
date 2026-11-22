package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import com.mowtiie.mesgard.ui.navigation.GroupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The minimum length of the reason, as the server asks for it. */
internal const val ACCESS_REASON_MIN_LENGTH = 10
internal const val ACCESS_REASON_MAX_LENGTH = 500

internal data class GroupAccessUiState(
    val isOpening: Boolean = false,
    /** True once the messages were opened, until the screen has gone there. */
    val isOpened: Boolean = false,
    val error: AdminError? = null,
)

/** Opens the messages of a group a person created, after the administrator wrote why. */
@HiltViewModel
internal class GroupAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GroupAccessRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupRoute>().groupId
    private val mutableUiState = MutableStateFlow(GroupAccessUiState())
    val uiState: StateFlow<GroupAccessUiState> = mutableUiState.asStateFlow()

    fun onOpenConfirmed(reason: String) {
        if (reason.trim().length < ACCESS_REASON_MIN_LENGTH || mutableUiState.value.isOpening) return
        mutableUiState.update { it.copy(isOpening = true) }
        viewModelScope.launch {
            val result = repository.open(groupId, reason)
            mutableUiState.update {
                it.copy(
                    isOpening = false,
                    isOpened = result is AdminResult.Success,
                    error = (result as? AdminResult.Failure)?.error,
                )
            }
        }
    }

    fun onOpenedHandled() = mutableUiState.update { it.copy(isOpened = false) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
