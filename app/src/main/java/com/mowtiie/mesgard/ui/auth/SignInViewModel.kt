package com.mowtiie.mesgard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.SignInResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class SignInViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = mutableUiState.asStateFlow()

    fun onEmailChanged(email: String) = mutableUiState.update { it.copy(email = email, error = null) }

    fun onPasswordChanged(password: String) = mutableUiState.update { it.copy(password = password, error = null) }

    fun onSignInClicked() {
        val current = mutableUiState.value
        if (!current.canSubmit) return
        mutableUiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(current.email.trim(), current.password)
            mutableUiState.update { it.copy(isSubmitting = false, error = (result as? SignInResult.Failure)?.error) }
        }
    }
}
