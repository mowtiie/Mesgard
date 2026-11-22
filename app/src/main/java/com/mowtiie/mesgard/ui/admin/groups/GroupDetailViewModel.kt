package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.ui.navigation.GroupRoute
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

internal data class GroupMemberRow(val userId: String, val displayName: String?, val role: GroupRole)

internal sealed interface GroupDetailDialog {
    data object Rename : GroupDetailDialog

    data object ConfirmDelete : GroupDetailDialog

    data class ConfirmRemove(val member: GroupMemberRow) : GroupDetailDialog
}

internal data class GroupDetailUiState(
    val name: String = "",
    val settings: GroupSettings = GroupSettings(),
    val members: List<GroupMemberRow> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val isDeleted: Boolean = false,
    val dialog: GroupDetailDialog? = null,
    val error: AdminError? = null,
)

@HiltViewModel
internal class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contactRepository: ContactRepository,
    private val groupAdminRepository: GroupAdminRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupRoute>().groupId
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<GroupDetailUiState> = combine(
        localState,
        contactRepository.observeContacts(),
    ) { local, contacts ->
        val names = contacts.associate { it.userId to it.displayName }
        GroupDetailUiState(
            name = local.group?.name.orEmpty(),
            settings = local.group?.settings ?: GroupSettings(),
            members = local.group?.members.orEmpty()
                .map { GroupMemberRow(it.userId, names[it.userId], it.role) }
                .sortedWith(compareBy({ it.role }, { it.displayName.orEmpty().lowercase() })),
            isLoading = local.isLoading,
            isWorking = local.isWorking,
            isDeleted = local.isDeleted,
            dialog = local.dialog,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), GroupDetailUiState())

    init {
        onRefresh()
    }

    fun onRefresh() {
        localState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = groupAdminRepository.fetchGroup(groupId)) {
                is AdminResult.Success -> localState.update { it.copy(group = result.value, isLoading = false) }
                is AdminResult.Failure -> localState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onDialogRequested(dialog: GroupDetailDialog) = localState.update { it.copy(dialog = dialog) }

    fun onDialogDismissed() = localState.update { it.copy(dialog = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    fun onRenameConfirmed(name: String) {
        if (name.isBlank()) return
        change { groupAdminRepository.renameGroup(groupId, name) }
    }

    fun onDeleteConfirmed() {
        change(onSuccess = { it.copy(isDeleted = true) }, refresh = false) { groupAdminRepository.deleteGroup(groupId) }
    }

    fun onSettingsChanged(settings: GroupSettings) {
        change { groupAdminRepository.changeSettings(groupId, settings) }
    }

    fun onRemoveConfirmed(member: GroupMemberRow) {
        change { groupAdminRepository.removeMember(groupId, member.userId) }
    }

    fun onRoleSelected(member: GroupMemberRow, role: GroupRole) {
        if (role == member.role) return
        change { groupAdminRepository.changeMemberRole(groupId, member.userId, role) }
    }

    private fun change(
        refresh: Boolean = true,
        onSuccess: (LocalState) -> LocalState = { it },
        action: suspend () -> AdminResult<Unit>,
    ) {
        if (localState.value.isWorking) return
        localState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val error = (action() as? AdminResult.Failure)?.error
            localState.update {
                val updated = if (error == null) onSuccess(it) else it
                updated.copy(isWorking = false, dialog = null, error = error)
            }
            if (error == null && refresh) onRefresh()
        }
    }

    private data class LocalState(
        val group: GroupDetail? = null,
        val isLoading: Boolean = true,
        val isWorking: Boolean = false,
        val isDeleted: Boolean = false,
        val dialog: GroupDetailDialog? = null,
        val error: AdminError? = null,
    )
}
