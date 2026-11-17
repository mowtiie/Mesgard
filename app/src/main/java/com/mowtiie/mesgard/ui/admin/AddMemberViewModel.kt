package com.mowtiie.mesgard.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.SkipReason
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class AddMemberError {
    EMAIL_INVALID,
    EMAIL_TAKEN,
    NAME_INVALID,
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    UNKNOWN,
}

/** The login that was just created. The password is shown once and is not kept anywhere else. */
internal data class CreatedLogin(val displayName: String, val temporaryPassword: String)

internal data class AddMemberUiState(
    val email: String = "",
    val displayName: String = "",
    val role: OrgRole = OrgRole.STUDENT,
    val isSaving: Boolean = false,
    val created: CreatedLogin? = null,
    val error: AddMemberError? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && !isSaving

    val isEmailError: Boolean
        get() = error == AddMemberError.EMAIL_INVALID || error == AddMemberError.EMAIL_TAKEN

    val isNameError: Boolean
        get() = error == AddMemberError.NAME_INVALID
}

@HiltViewModel
internal class AddMemberViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val adminRepository: AdminRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AddMemberUiState())
    val uiState: StateFlow<AddMemberUiState> = mutableUiState.asStateFlow()

    fun onEmailChanged(email: String) = mutableUiState.update { it.copy(email = email, error = null) }

    fun onDisplayNameChanged(name: String) = mutableUiState.update { it.copy(displayName = name, error = null) }

    fun onRoleSelected(role: OrgRole) = mutableUiState.update { it.copy(role = role) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onAddClicked() {
        val state = mutableUiState.value
        if (!state.canSubmit) return
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val outcome = organizationId?.let { create(it, state) } ?: Outcome(error = AddMemberError.NOT_ALLOWED)
            if (outcome.created != null) contactRepository.refresh()
            mutableUiState.update { it.copy(isSaving = false, created = outcome.created, error = outcome.error) }
        }
    }

    private suspend fun create(organizationId: String, state: AddMemberUiState): Outcome {
        val member = NewMember(state.email.trim(), state.displayName.trim(), state.role)
        return when (val result = adminRepository.createMembers(organizationId, listOf(member))) {
            is AdminResult.Failure -> Outcome(error = result.error.toAddMemberError())
            is AdminResult.Success -> when (val creation = result.value.singleOrNull()) {
                is MemberCreation.Created -> Outcome(
                    created = CreatedLogin(
                        state.displayName.trim().ifEmpty {
                            creation.email
                        },
                        creation.temporaryPassword,
                    ),
                )
                is MemberCreation.Skipped -> Outcome(error = creation.reason.toAddMemberError())
                null -> Outcome(error = AddMemberError.UNKNOWN)
            }
        }
    }

    fun onCreatedHandled() = mutableUiState.update { it.copy(created = null) }

    private data class Outcome(val error: AddMemberError? = null, val created: CreatedLogin? = null)
}

private fun AdminError.toAddMemberError() = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> AddMemberError.NETWORK_UNAVAILABLE
    AdminError.NOT_ALLOWED -> AddMemberError.NOT_ALLOWED
    AdminError.NOT_A_MEMBER, AdminError.LAST_ADMIN, AdminError.NOT_ERASABLE, AdminError.UNKNOWN ->
        AddMemberError.UNKNOWN
}

private fun SkipReason.toAddMemberError() = when (this) {
    SkipReason.EMAIL_TAKEN -> AddMemberError.EMAIL_TAKEN
    SkipReason.INVALID_EMAIL -> AddMemberError.EMAIL_INVALID
    SkipReason.INVALID_NAME -> AddMemberError.NAME_INVALID
    SkipReason.DUPLICATE, SkipReason.INVALID_ROLE, SkipReason.FAILED -> AddMemberError.UNKNOWN
}
