package com.mowtiie.mesgard.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.ui.common.matchesQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class MembersViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val contactRepository: ContactRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<MembersUiState> = combine(
        observeAdminOrganization.organization,
        contactRepository.observeContacts(),
        localState,
    ) { organization, contacts, local ->
        MembersUiState(
            organizationName = organization?.organizationName,
            members = contacts.filter {
                it.orgId == organization?.organizationId && matchesQuery(it.displayName, local.query)
            },
            query = local.query,
            isRefreshing = local.isRefreshing,
            isWorking = local.isWorking,
            dialog = local.dialog,
            erasureMode = local.erasureMode,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), MembersUiState())

    init {
        onRefresh()
        viewModelScope.launch {
            val mode = (adminRepository.erasureMode() as? AdminResult.Success)?.value ?: ErasureMode.OFF
            localState.update { it.copy(erasureMode = mode) }
        }
    }

    fun onRefresh() {
        localState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val failure = (contactRepository.refresh() as? RefreshResult.Failure)?.error
            localState.update { it.copy(isRefreshing = false, error = failure?.toMembersError()) }
        }
    }

    fun onQueryChanged(query: String) = localState.update { it.copy(query = query) }

    fun onMemberAction(member: Contact, action: MemberAction) = localState.update {
        it.copy(
            dialog = when (action) {
                MemberAction.CHANGE_ROLE -> MembersDialog.ChangeRole(member, selected = member.role)
                MemberAction.RESET_PASSWORD -> MembersDialog.ConfirmResetPassword(member)
                MemberAction.RESET_MFA -> MembersDialog.ConfirmResetMfa(member)
                MemberAction.REMOVE -> MembersDialog.ConfirmRemove(member)
            },
        )
    }

    fun onRoleSelected(role: OrgRole) = localState.update { state ->
        val dialog = state.dialog as? MembersDialog.ChangeRole
        state.copy(dialog = dialog?.copy(selected = role) ?: state.dialog)
    }

    fun onEraseToggled(erase: Boolean) = localState.update { state ->
        val dialog = state.dialog as? MembersDialog.ConfirmRemove
        state.copy(dialog = dialog?.copy(erase = erase) ?: state.dialog)
    }

    fun onDialogDismissed() = localState.update { it.copy(dialog = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    fun onDialogConfirmed() {
        val dialog = localState.value.dialog
        if (dialog == null || localState.value.isWorking) return
        localState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val outcome = organizationId?.let { perform(it, dialog) } ?: Outcome(error = MembersError.NOT_ALLOWED)
            if (outcome.error == null) contactRepository.refresh()
            localState.update { it.copy(isWorking = false, dialog = outcome.dialog, error = outcome.error) }
        }
    }

    private suspend fun perform(organizationId: String, dialog: MembersDialog): Outcome = when (dialog) {
        is MembersDialog.ChangeRole ->
            adminRepository.changeRole(organizationId, dialog.member.userId, dialog.selected).toOutcome { null }
        is MembersDialog.ConfirmResetPassword ->
            adminRepository.resetPassword(organizationId, dialog.member.userId).toOutcome { password ->
                MembersDialog.TemporaryPassword(dialog.member.displayName, password)
            }
        is MembersDialog.ConfirmResetMfa ->
            adminRepository.resetMfa(organizationId, dialog.member.userId).toOutcome { null }
        is MembersDialog.ConfirmRemove -> if (dialog.erase) {
            adminRepository.eraseMember(organizationId, dialog.member.userId).toOutcome { null }
        } else {
            adminRepository.removeMember(organizationId, dialog.member.userId).toOutcome { null }
        }
        is MembersDialog.TemporaryPassword -> Outcome()
    }

    private data class Outcome(val dialog: MembersDialog? = null, val error: MembersError? = null)

    private data class LocalState(
        val query: String = "",
        val isRefreshing: Boolean = false,
        val isWorking: Boolean = false,
        val dialog: MembersDialog? = null,
        val erasureMode: ErasureMode = ErasureMode.OFF,
        val error: MembersError? = null,
    )

    private fun <T> AdminResult<T>.toOutcome(next: (T) -> MembersDialog?): Outcome = when (this) {
        is AdminResult.Success -> Outcome(dialog = next(value))
        is AdminResult.Failure -> Outcome(error = error.toMembersError())
    }
}

private fun RefreshError.toMembersError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> MembersError.REFRESH_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> MembersError.REFRESH_UNKNOWN
}

internal fun AdminError.toMembersError() = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> MembersError.NETWORK_UNAVAILABLE
    AdminError.NOT_ALLOWED -> MembersError.NOT_ALLOWED
    AdminError.NOT_A_MEMBER -> MembersError.NOT_A_MEMBER
    AdminError.LAST_ADMIN -> MembersError.LAST_ADMIN
    AdminError.NOT_ERASABLE, AdminError.LEGAL_HOLD -> MembersError.NOT_ERASABLE
    AdminError.UNKNOWN -> MembersError.UNKNOWN
}
