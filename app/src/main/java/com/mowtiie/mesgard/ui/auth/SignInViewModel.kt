package com.mowtiie.mesgard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.domain.model.SignInResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    serverConfigStore: ServerConfigStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignInUiState(serverHost = serverConfigStore.current?.host.orEmpty()))
    val uiState: StateFlow<SignInUiState> = mutableUiState.asStateFlow()

    fun onEmailChanged(email: String) = mutableUiState.update { it.copy(email = email, error = null) }

    fun onPasswordChanged(password: String) = mutableUiState.update { it.copy(password = password, error = null) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onSignInClicked() {
        val current = mutableUiState.value
        if (!current.canSubmit) return
        mutableUiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(current.email.trim(), current.password)
            val error = (result as? SignInResult.Failure)?.error
            mutableUiState.update {
                it.copy(
                    isSubmitting = false,
                    error = error,
                    password = if (error ==
                        SignInError.INVALID_CREDENTIALS
                    ) {
                        ""
                    } else {
                        it.password
                    },
                )
            }
        }
    }
}
