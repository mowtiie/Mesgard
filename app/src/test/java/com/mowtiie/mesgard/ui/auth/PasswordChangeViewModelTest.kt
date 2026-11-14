package com.mowtiie.mesgard.ui.auth

import com.mowtiie.mesgard.domain.model.PasswordChangeError
import com.mowtiie.mesgard.domain.model.PasswordChangeResult
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

class PasswordChangeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { PasswordChangeViewModel(authRepository) }

    private fun fill(password: String = NEW_PASSWORD, confirmation: String = NEW_PASSWORD) {
        viewModel.onPasswordChanged(password)
        viewModel.onConfirmationChanged(confirmation)
    }

    @Test
    fun `cannot save before both fields are filled`() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChanged(NEW_PASSWORD)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onConfirmationChanged(NEW_PASSWORD)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `saves a valid password through the repository`() {
        coEvery { authRepository.changePassword(NEW_PASSWORD) } returns PasswordChangeResult.Success
        fill()

        viewModel.onSaveClicked()

        coVerify(exactly = 1) { authRepository.changePassword(NEW_PASSWORD) }
        assertNull(viewModel.uiState.value.issue)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `refuses a short password and a mismatch without asking the server`() {
        fill(password = "short", confirmation = "short")
        viewModel.onSaveClicked()
        assertEquals(PasswordChangeIssue.TOO_SHORT, viewModel.uiState.value.issue)
        assertTrue(viewModel.uiState.value.isPasswordIssue)

        fill(password = NEW_PASSWORD, confirmation = "something-else")
        viewModel.onSaveClicked()
        assertEquals(PasswordChangeIssue.MISMATCH, viewModel.uiState.value.issue)
        assertTrue(viewModel.uiState.value.isConfirmationIssue)

        coVerify(exactly = 0) { authRepository.changePassword(any()) }
    }

    @Test
    fun `shows the reason when the server refuses the password`() {
        coEvery { authRepository.changePassword(NEW_PASSWORD) } returns
            PasswordChangeResult.Failure(PasswordChangeError.WEAK_PASSWORD)
        fill()

        viewModel.onSaveClicked()

        assertEquals(PasswordChangeIssue.WEAK_PASSWORD, viewModel.uiState.value.issue)
        assertTrue(viewModel.uiState.value.isPasswordIssue)
    }

    @Test
    fun `maps the other failures`() {
        coEvery { authRepository.changePassword(NEW_PASSWORD) } returns
            PasswordChangeResult.Failure(PasswordChangeError.SAME_PASSWORD)
        fill()
        viewModel.onSaveClicked()
        assertEquals(PasswordChangeIssue.SAME_PASSWORD, viewModel.uiState.value.issue)

        coEvery { authRepository.changePassword(NEW_PASSWORD) } returns
            PasswordChangeResult.Failure(PasswordChangeError.NETWORK_UNAVAILABLE)
        viewModel.onSaveClicked()
        assertEquals(PasswordChangeIssue.NETWORK_UNAVAILABLE, viewModel.uiState.value.issue)
        assertFalse(viewModel.uiState.value.isPasswordIssue)

        viewModel.onIssueShown()
        assertNull(viewModel.uiState.value.issue)
    }

    @Test
    fun `ignores a second tap while saving`() {
        val gate = CompletableDeferred<PasswordChangeResult>()
        coEvery { authRepository.changePassword(NEW_PASSWORD) } coAnswers { gate.await() }
        fill()

        viewModel.onSaveClicked()
        viewModel.onSaveClicked()

        assertTrue(viewModel.uiState.value.isSaving)
        coVerify(exactly = 1) { authRepository.changePassword(NEW_PASSWORD) }
        gate.complete(PasswordChangeResult.Success)
    }

    @Test
    fun `signs out from the screen`() {
        coEvery { authRepository.signOut() } returns Unit

        viewModel.onSignOutClicked()

        coVerify(exactly = 1) { authRepository.signOut() }
    }

    private companion object {
        const val NEW_PASSWORD = "my-own-password"
    }
}
