package com.mowtiie.mesgard.ui.auth

import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.domain.model.SignInResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { SignInViewModel(authRepository) }

    @Test
    fun `cannot submit while the email or password is empty`() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChanged(EMAIL)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChanged(PASSWORD)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `signs in with the trimmed email and the password`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Success
        viewModel.onEmailChanged("  $EMAIL  ")
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()

        coVerify(exactly = 1) { authRepository.signIn(EMAIL, PASSWORD) }
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `shows the failure reason when sign in is rejected`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Failure(SignInError.INVALID_CREDENTIALS)
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()

        assertEquals(SignInError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `clears the error when the user edits a field`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Failure(SignInError.NETWORK_UNAVAILABLE)
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)
        viewModel.onSignInClicked()

        viewModel.onPasswordChanged("another")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clears the error once it has been shown`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Failure(SignInError.NETWORK_UNAVAILABLE)
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)
        viewModel.onSignInClicked()

        viewModel.onErrorShown()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `ignores a second tap while a sign in is in progress`() {
        val pending = CompletableDeferred<SignInResult>()
        coEvery { authRepository.signIn(any(), any()) } coAnswers { pending.await() }
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()
        viewModel.onSignInClicked()
        pending.complete(SignInResult.Success)

        coVerify(exactly = 1) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `reports submitting while a sign in is in progress`() {
        val pending = CompletableDeferred<SignInResult>()
        coEvery { authRepository.signIn(any(), any()) } coAnswers { pending.await() }
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()

        assertTrue(viewModel.uiState.value.isSubmitting)
        pending.complete(SignInResult.Success)
    }

    private companion object {
        const val EMAIL = "student1@rnhs.test"
        const val PASSWORD = "password123"
    }
}
