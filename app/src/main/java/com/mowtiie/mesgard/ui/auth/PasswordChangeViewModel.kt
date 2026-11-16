package com.mowtiie.mesgard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.auth.PasswordProblem
import com.mowtiie.mesgard.domain.auth.findPasswordProblem
import com.mowtiie.mesgard.domain.model.PasswordChangeError
import com.mowtiie.mesgard.domain.model.PasswordChangeResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class PasswordChangeIssue {
    TOO_SHORT,
    MISMATCH,
    WEAK_PASSWORD,
    SAME_PASSWORD,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}

internal data class PasswordChangeUiState(
    val password: String = "",
    val confirmation: String = "",
    val isSaving: Boolean = false,
    val issue: PasswordChangeIssue? = null,
    val isChanged: Boolean = false,
) {
    val canSubmit: Boolean
        get() = password.isNotEmpty() && confirmation.isNotEmpty() && !isSaving

    val isPasswordIssue: Boolean
        get() = issue in setOf(
            PasswordChangeIssue.TOO_SHORT,
            PasswordChangeIssue.WEAK_PASSWORD,
            PasswordChangeIssue.SAME_PASSWORD,
        )

    val isConfirmationIssue: Boolean
        get() = issue == PasswordChangeIssue.MISMATCH
}

/**
 * Changes the password of the signed in person: right after sign in while the account still has the temporary password
 * an admin handed out, and later from the Account screen.
 */
@HiltViewModel
internal class PasswordChangeViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PasswordChangeUiState())
    val uiState: StateFlow<PasswordChangeUiState> = mutableUiState.asStateFlow()

    fun onPasswordChanged(password: String) = mutableUiState.update { it.copy(password = password, issue = null) }

    fun onConfirmationChanged(confirmation: String) =
        mutableUiState.update { it.copy(confirmation = confirmation, issue = null) }

    fun onIssueShown() = mutableUiState.update { it.copy(issue = null) }

    fun onChangeAnnounced() = mutableUiState.update { it.copy(isChanged = false) }

    fun onSignOutClicked() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun onSaveClicked() {
        val state = mutableUiState.value
        if (!state.canSubmit) return
        val problem = findPasswordProblem(state.password, state.confirmation)
        if (problem != null) {
            mutableUiState.update { it.copy(issue = problem.toIssue()) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, issue = null) }
        viewModelScope.launch {
            val issue = (authRepository.changePassword(state.password) as? PasswordChangeResult.Failure)
                ?.error?.toIssue()
            mutableUiState.update {
                if (issue == null) {
                    PasswordChangeUiState(isChanged = true)
                } else {
                    it.copy(isSaving = false, issue = issue)
                }
            }
        }
    }
}

private fun PasswordProblem.toIssue() = when (this) {
    PasswordProblem.TOO_SHORT -> PasswordChangeIssue.TOO_SHORT
    PasswordProblem.MISMATCH -> PasswordChangeIssue.MISMATCH
}

private fun PasswordChangeError.toIssue() = when (this) {
    PasswordChangeError.WEAK_PASSWORD -> PasswordChangeIssue.WEAK_PASSWORD
    PasswordChangeError.SAME_PASSWORD -> PasswordChangeIssue.SAME_PASSWORD
    PasswordChangeError.NETWORK_UNAVAILABLE -> PasswordChangeIssue.NETWORK_UNAVAILABLE
    PasswordChangeError.UNKNOWN -> PasswordChangeIssue.UNKNOWN
}
