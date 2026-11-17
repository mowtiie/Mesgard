package com.mowtiie.mesgard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MfaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SETUP_KEY_GROUP_SIZE = 4

/** The setup key in groups of four, which is easier to read and to type into an authenticator app. */
internal fun formatSetupKey(secret: String): String = secret.chunked(SETUP_KEY_GROUP_SIZE).joinToString(" ")

internal data class MfaEnrollmentUiState(
    val enrollment: MfaEnrollment? = null,
    val isLoading: Boolean = true,
    val code: String = "",
    val isVerifying: Boolean = false,
    val issue: MfaIssue? = null,
    val isEnrolled: Boolean = false,
) {
    val canSubmit: Boolean
        get() = enrollment != null && code.length == MFA_CODE_LENGTH && !isVerifying

    val hasFailedToStart: Boolean
        get() = enrollment == null && !isLoading
}

/** Sets up the second factor an account must have: creates the factor, then confirms the first code. */
@HiltViewModel
internal class MfaEnrollmentViewModel @Inject constructor(
    private val mfaRepository: MfaRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MfaEnrollmentUiState())
    val uiState: StateFlow<MfaEnrollmentUiState> = mutableUiState.asStateFlow()

    init {
        start()
    }

    fun onRetryClicked() {
        mutableUiState.update { MfaEnrollmentUiState() }
        start()
    }

    fun onCodeChanged(input: String) {
        if (mutableUiState.value.isVerifying) return
        mutableUiState.update { it.copy(code = digitsOfCode(input), issue = null) }
        onConfirmClicked()
    }

    fun onConfirmClicked() {
        val state = mutableUiState.value
        val enrollment = state.enrollment
        if (!state.canSubmit || enrollment == null) return
        mutableUiState.update { it.copy(isVerifying = true, issue = null) }
        viewModelScope.launch {
            val result = mfaRepository.confirmEnrollment(enrollment, state.code)
            mutableUiState.update {
                when (result) {
                    is MfaResult.Success -> it.copy(code = "", isVerifying = false, isEnrolled = true)
                    is MfaResult.Failure -> it.copy(code = "", isVerifying = false, issue = result.error.toIssue())
                }
            }
        }
    }

    fun onSignOutClicked() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private fun start() {
        viewModelScope.launch {
            val result = mfaRepository.startEnrollment()
            mutableUiState.update {
                when (result) {
                    is MfaResult.Success -> it.copy(enrollment = result.value, isLoading = false)
                    is MfaResult.Failure -> it.copy(isLoading = false, issue = result.error.toIssue())
                }
            }
        }
    }
}
