package com.mowtiie.mesgard.ui.settings

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.push.PushRegistration
import com.mowtiie.mesgard.domain.repository.NotificationSettingsRepository
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val deviceSettings = FakeDeviceSettings()
    private val notificationSettings = mockk<NotificationSettingsRepository> {
        coEvery { refresh() } returns RefreshResult.Success
        coEvery { setPreview(any()) } returns RefreshResult.Success
    }

    private val distributorName = MutableStateFlow<String?>(null)
    private val pushRegistration = mockk<PushRegistration>(relaxed = true) {
        every { this@mockk.distributorName } returns this@SettingsViewModelTest.distributorName
    }

    private fun viewModel() = SettingsViewModel(deviceSettings, notificationSettings, pushRegistration)

    @Test
    fun `refreshes the notification preview when it is created`() = runTest {
        viewModel().uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { notificationSettings.refresh() }
    }

    @Test
    fun `exposes the device settings`() = runTest {
        viewModel().uiState.test {
            val settings = expectMostRecentItem().settings
            assertTrue(settings.screenPrivacyEnabled)
            assertFalse(settings.appLockEnabled)
        }
    }

    @Test
    fun `changes the screen privacy setting`() = runTest {
        viewModel().onScreenPrivacyChanged(false)

        assertFalse(deviceSettings.settings.value.screenPrivacyEnabled)
    }

    @Test
    fun `changes the app lock setting and the lock timeout`() = runTest {
        val viewModel = viewModel()

        viewModel.onAppLockChanged(true)
        viewModel.onLockTimeoutSelected(LockTimeout.FIVE_MINUTES)

        assertTrue(deviceSettings.settings.value.appLockEnabled)
        assertEquals(LockTimeout.FIVE_MINUTES, deviceSettings.settings.value.lockTimeout)
    }

    @Test
    fun `reports that app lock needs a device screen lock and clears the message once shown`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onAppLockUnavailable()
            assertEquals(SettingsError.DEVICE_LOCK_REQUIRED, awaitItem().error)
            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `saves the selected notification preview`() = runTest {
        viewModel().onNotificationPreviewSelected(NotificationPreview.HIDDEN)

        coVerify(exactly = 1) { notificationSettings.setPreview(NotificationPreview.HIDDEN) }
    }

    @Test
    fun `reports a failure to save the notification preview`() = runTest {
        coEvery { notificationSettings.setPreview(any()) } returns
            RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()

        viewModel.onNotificationPreviewSelected(NotificationPreview.HIDDEN)

        viewModel.uiState.test {
            assertEquals(SettingsError.PREVIEW_NETWORK_UNAVAILABLE, expectMostRecentItem().error)
        }
    }

    @Test
    fun `shows the push distributor the user chose`() = runTest {
        distributorName.value = "ntfy"

        viewModel().uiState.test { assertEquals("ntfy", expectMostRecentItem().pushDistributorName) }
    }

    @Test
    fun `shows that push is not set up when no distributor is chosen`() = runTest {
        viewModel().uiState.test { assertNull(expectMostRecentItem().pushDistributorName) }
    }

    @Test
    fun `registers with the distributor once the user has chosen one`() = runTest {
        viewModel().onPushDistributorChosen()

        verify(exactly = 1) { pushRegistration.onDistributorChosen() }
    }

    @Test
    fun `reports that no push distributor is installed`() = runTest {
        val viewModel = viewModel()

        viewModel.onPushDistributorUnavailable()

        viewModel.uiState.test {
            assertEquals(SettingsError.PUSH_DISTRIBUTOR_UNAVAILABLE, expectMostRecentItem().error)
        }
    }
}
