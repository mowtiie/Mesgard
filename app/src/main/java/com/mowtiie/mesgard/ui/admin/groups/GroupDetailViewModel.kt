package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
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

    data object ConfirmLeave : GroupDetailDialog

    data object OnlyOwner : GroupDetailDialog
}

internal data class GroupDetailUiState(
    val name: String = "",
    val settings: GroupSettings = GroupSettings(),
    val members: List<GroupMemberRow> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val isDeleted: Boolean = false,
    val hasLeft: Boolean = false,
    val isAdmin: Boolean = false,
    val isUserCreated: Boolean = false,
    val ownRole: GroupRole? = null,
    val avatar: AvatarRef? = null,
    val orgId: String = "",
    val dialog: GroupDetailDialog? = null,
    val error: AdminError? = null,
) {
    /** Organization admins and owners of the group change its name and its roster. */
    val canManage: Boolean
        get() = isAdmin || ownRole == GroupRole.OWNER

    /** Administrators and the owners and moderators of the group set its picture. */
    val canChangePicture: Boolean
        get() = isAdmin || ownRole == GroupRole.OWNER || ownRole == GroupRole.MODERATOR

    /** The owner of a group cannot report it; everyone else in the organization can. */
    val canReport: Boolean
        get() = !isLoading && ownRole != GroupRole.OWNER

    val isOnlyOwner: Boolean
        get() = ownRole == GroupRole.OWNER && members.count { it.role == GroupRole.OWNER } == 1
}

@HiltViewModel
internal class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contactRepository: ContactRepository,
    private val groupAdminRepository: GroupAdminRepository,
    private val authRepository: AuthRepository,
    observeAdminOrganization: ObserveAdminOrganization,
    accountRepository: AccountRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupRoute>().groupId
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<GroupDetailUiState> = combine(
        localState,
        contactRepository.observeContacts(),
        observeAdminOrganization.organization,
        accountRepository.observeAccount(),
    ) { local, contacts, adminOrganization, account ->
        // The roster leaves out the signed in person, so their own name comes from the account.
        val own = authRepository.currentUserId?.let { id -> account?.let { mapOf(id to it.displayName) } }.orEmpty()
        val names = own + contacts.associate { it.userId to it.displayName }
        GroupDetailUiState(
            name = local.group?.name.orEmpty(),
            settings = local.group?.settings ?: GroupSettings(),
            members = local.group?.members.orEmpty()
                .map { GroupMemberRow(it.userId, names[it.userId], it.role) }
                .sortedWith(compareBy({ it.role }, { it.displayName.orEmpty().lowercase() })),
            isLoading = local.isLoading,
            isWorking = local.isWorking,
            isDeleted = local.isDeleted,
            hasLeft = local.hasLeft,
            isAdmin = adminOrganization != null,
            isUserCreated = local.group?.isUserCreated == true,
            ownRole = local.group?.members?.firstOrNull { it.userId == authRepository.currentUserId }?.role,
            avatar = local.group?.avatar,
            orgId = local.group?.orgId.orEmpty(),
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

    /** Opens a dialog, or closes the open one when [dialog] is null. */
    fun onDialogChanged(dialog: GroupDetailDialog?) = localState.update { it.copy(dialog = dialog) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    fun onRenameConfirmed(name: String) {
        if (name.isBlank()) return
        change { groupAdminRepository.renameGroup(groupId, name) }
    }

    fun onDeleteConfirmed() {
        change(onSuccess = { it.copy(isDeleted = true) }, refresh = false) { groupAdminRepository.deleteGroup(groupId) }
    }

    fun onLeaveConfirmed() {
        val userId = authRepository.currentUserId ?: return
        change(onSuccess = { it.copy(hasLeft = true) }, refresh = false) {
            groupAdminRepository.removeMember(groupId, userId)
        }
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
        val hasLeft: Boolean = false,
        val dialog: GroupDetailDialog? = null,
        val error: AdminError? = null,
    )
}
