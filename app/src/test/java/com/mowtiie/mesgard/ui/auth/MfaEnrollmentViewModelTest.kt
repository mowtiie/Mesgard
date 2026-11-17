package com.mowtiie.mesgard.ui.auth

import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MfaRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MfaEnrollmentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val enrollment = MfaEnrollment("factor-1", "JBSWY3DPEHPK3PXP", "otpauth://totp/Mesgard:a@b.c?secret=x")
    private val mfaRepository = mockk<MfaRepository>()
    private val authRepository = mockk<AuthRepository>()

    private fun viewModel() = MfaEnrollmentViewModel(mfaRepository, authRepository)

    @Test
    fun `groups the setup key in fours`() {
        assertEquals("JBSW Y3DP EHPK 3PXP", formatSetupKey("JBSWY3DPEHPK3PXP"))
        assertEquals("ABCD EF", formatSetupKey("ABCDEF"))
    }

    @Test
    fun `starts an enrollment when it opens`() {
        coEvery { mfaRepository.startEnrollment() } returns MfaResult.Success(enrollment)

        val state = viewModel().uiState.value

        assertEquals(enrollment, state.enrollment)
        assertFalse(state.isLoading)
        assertFalse(state.hasFailedToStart)
    }

    @Test
    fun `offers a retry when the enrollment cannot start`() {
        coEvery { mfaRepository.startEnrollment() } returnsMany listOf(
            MfaResult.Failure(MfaError.NETWORK_UNAVAILABLE),
            MfaResult.Success(enrollment),
        )
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.hasFailedToStart)
        assertEquals(MfaIssue.NETWORK_UNAVAILABLE, viewModel.uiState.value.issue)

        viewModel.onRetryClicked()

        assertEquals(enrollment, viewModel.uiState.value.enrollment)
        assertNull(viewModel.uiState.value.issue)
    }

    @Test
    fun `confirms the first code when the sixth digit arrives`() {
        coEvery { mfaRepository.startEnrollment() } returns MfaResult.Success(enrollment)
        coEvery { mfaRepository.confirmEnrollment(enrollment, CODE) } returns MfaResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onCodeChanged("123 456")

        coVerify(exactly = 1) { mfaRepository.confirmEnrollment(enrollment, CODE) }
        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `shows a wrong first code and keeps the setup key`() {
        coEvery { mfaRepository.startEnrollment() } returns MfaResult.Success(enrollment)
        coEvery { mfaRepository.confirmEnrollment(enrollment, CODE) } returns MfaResult.Failure(MfaError.INVALID_CODE)
        val viewModel = viewModel()

        viewModel.onCodeChanged(CODE)

        assertEquals(MfaIssue.INVALID_CODE, viewModel.uiState.value.issue)
        assertEquals("", viewModel.uiState.value.code)
        assertEquals(enrollment, viewModel.uiState.value.enrollment)
    }

    @Test
    fun `cannot confirm before the enrollment exists`() {
        coEvery { mfaRepository.startEnrollment() } returns MfaResult.Failure(MfaError.UNKNOWN)
        val viewModel = viewModel()

        viewModel.onCodeChanged(CODE)

        coVerify(exactly = 0) { mfaRepository.confirmEnrollment(any(), any()) }
    }

    @Test
    fun `never prints the secret`() {
        assertFalse(enrollment.toString().contains("JBSW"))
        assertFalse(MfaEnrollmentUiState(enrollment = enrollment).toString().contains("JBSW"))
    }

    @Test
    fun `signs out through the repository`() {
        coEvery { mfaRepository.startEnrollment() } returns MfaResult.Success(enrollment)
        coEvery { authRepository.signOut() } returns Unit

        viewModel().onSignOutClicked()

        coVerify(exactly = 1) { authRepository.signOut() }
    }

    private companion object {
        const val CODE = "123456"
    }
}
