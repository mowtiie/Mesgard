package com.mowtiie.mesgard.ui.auth

import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MfaRepository
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

class MfaCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mfaRepository = mockk<MfaRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { MfaCodeViewModel(mfaRepository, authRepository) }

    @Test
    fun `keeps only the digits and at most six of them`() {
        coEvery { mfaRepository.verifySignIn(any()) } returns MfaResult.Success(Unit)

        viewModel.onCodeChanged("12 34")
        assertEquals("1234", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onCodeChanged("12a34b5")
        assertEquals("12345", viewModel.uiState.value.code)
    }

    @Test
    fun `submits as soon as the sixth digit arrives`() {
        coEvery { mfaRepository.verifySignIn(CODE) } returns MfaResult.Success(Unit)

        viewModel.onCodeChanged("123 456")

        coVerify(exactly = 1) { mfaRepository.verifySignIn(CODE) }
        assertEquals("", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `does not submit an incomplete code`() {
        viewModel.onCodeChanged("12345")
        viewModel.onSubmitClicked()

        coVerify(exactly = 0) { mfaRepository.verifySignIn(any()) }
    }

    @Test
    fun `shows a wrong code and clears the field`() {
        coEvery { mfaRepository.verifySignIn(CODE) } returns MfaResult.Failure(MfaError.INVALID_CODE)

        viewModel.onCodeChanged(CODE)

        assertEquals(MfaIssue.INVALID_CODE, viewModel.uiState.value.issue)
        assertEquals("", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `clears the issue when the person types again`() {
        coEvery { mfaRepository.verifySignIn(CODE) } returns MfaResult.Failure(MfaError.NETWORK_UNAVAILABLE)
        viewModel.onCodeChanged(CODE)
        assertEquals(MfaIssue.NETWORK_UNAVAILABLE, viewModel.uiState.value.issue)

        viewModel.onCodeChanged("1")

        assertNull(viewModel.uiState.value.issue)
    }

    @Test
    fun `ignores typing while a code is being checked`() {
        val pending = CompletableDeferred<MfaResult<Unit>>()
        coEvery { mfaRepository.verifySignIn(CODE) } coAnswers { pending.await() }
        viewModel.onCodeChanged(CODE)
        assertTrue(viewModel.uiState.value.isVerifying)

        viewModel.onCodeChanged("999999")

        assertEquals(CODE, viewModel.uiState.value.code)
        coVerify(exactly = 1) { mfaRepository.verifySignIn(any()) }
        pending.complete(MfaResult.Success(Unit))
    }

    @Test
    fun `signs out through the repository`() {
        coEvery { authRepository.signOut() } returns Unit

        viewModel.onSignOutClicked()

        coVerify(exactly = 1) { authRepository.signOut() }
    }

    private companion object {
        const val CODE = "123456"
    }
}
