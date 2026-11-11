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

    @Test
    fun `clears the password after the credentials are rejected but keeps the email`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Failure(SignInError.INVALID_CREDENTIALS)
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()

        assertEquals("", viewModel.uiState.value.password)
        assertEquals(EMAIL, viewModel.uiState.value.email)
    }

    @Test
    fun `keeps the password after a failure that is not about the credentials`() {
        coEvery { authRepository.signIn(any(), any()) } returns SignInResult.Failure(SignInError.NETWORK_UNAVAILABLE)
        viewModel.onEmailChanged(EMAIL)
        viewModel.onPasswordChanged(PASSWORD)

        viewModel.onSignInClicked()

        assertEquals(PASSWORD, viewModel.uiState.value.password)
    }

    @Test
    fun `shows the host of the server`() {
        assertFalse(viewModel.uiState.value.serverHost.isBlank())
        assertEquals("school.example", serverHostOf("https://school.example/path"))
        assertEquals("10.0.2.2", serverHostOf("http://10.0.2.2:54321"))
        assertEquals("not a url", serverHostOf("not a url"))
    }

    private companion object {
        const val EMAIL = "student1@rnhs.test"
        const val PASSWORD = "password123"
    }
}
