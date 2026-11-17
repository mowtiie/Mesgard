package com.mowtiie.mesgard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.MfaError
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

internal const val MFA_CODE_LENGTH = 6

internal enum class MfaIssue {
    INVALID_CODE,
    TOO_MANY_ATTEMPTS,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}

internal fun MfaError.toIssue(): MfaIssue = when (this) {
    MfaError.INVALID_CODE -> MfaIssue.INVALID_CODE
    MfaError.TOO_MANY_ATTEMPTS -> MfaIssue.TOO_MANY_ATTEMPTS
    MfaError.NETWORK_UNAVAILABLE -> MfaIssue.NETWORK_UNAVAILABLE
    MfaError.UNKNOWN -> MfaIssue.UNKNOWN
}

/** Keeps the digits the person typed, at most one code's worth, so pasting "123 456" works. */
internal fun digitsOfCode(input: String): String = input.filter(Char::isDigit).take(MFA_CODE_LENGTH)

internal data class MfaCodeUiState(
    val code: String = "",
    val isVerifying: Boolean = false,
    val issue: MfaIssue? = null,
) {
    val canSubmit: Boolean
        get() = code.length == MFA_CODE_LENGTH && !isVerifying
}

/** Asks for the code of an account that has a second factor. Success changes the session, which moves the root on. */
@HiltViewModel
internal class MfaCodeViewModel @Inject constructor(
    private val mfaRepository: MfaRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MfaCodeUiState())
    val uiState: StateFlow<MfaCodeUiState> = mutableUiState.asStateFlow()

    fun onCodeChanged(input: String) {
        if (mutableUiState.value.isVerifying) return
        mutableUiState.update { it.copy(code = digitsOfCode(input), issue = null) }
        onSubmitClicked()
    }

    fun onSubmitClicked() {
        val state = mutableUiState.value
        if (!state.canSubmit) return
        mutableUiState.update { it.copy(isVerifying = true, issue = null) }
        viewModelScope.launch {
            val result = mfaRepository.verifySignIn(state.code)
            mutableUiState.update {
                when (result) {
                    is MfaResult.Success -> MfaCodeUiState()
                    is MfaResult.Failure -> MfaCodeUiState(issue = result.error.toIssue())
                }
            }
        }
    }

    fun onSignOutClicked() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
