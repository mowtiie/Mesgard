package com.mowtiie.mesgard.ui.lock

import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LockViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appLock = mockk<AppLock>(relaxed = true)
    private val authRepository = mockk<AuthRepository> { coEvery { signOut() } returns Unit }
    private val deviceSettings = FakeDeviceSettings(DeviceSettings(appLockEnabled = true))
    private val viewModel = LockViewModel(appLock, deviceSettings, authRepository)

    @Test
    fun `unlocks the app after the user authenticates`() {
        viewModel.onAuthenticated()

        verify(exactly = 1) { appLock.unlock() }
        coVerify(exactly = 0) { authRepository.signOut() }
    }

    @Test
    fun `signs the user out and turns app lock off when authentication is no longer possible`() {
        viewModel.onAuthenticationUnavailable()

        assertFalse(deviceSettings.settings.value.appLockEnabled)
        verify(exactly = 1) { appLock.unlock() }
        coVerify(exactly = 1) { authRepository.signOut() }
    }
}
