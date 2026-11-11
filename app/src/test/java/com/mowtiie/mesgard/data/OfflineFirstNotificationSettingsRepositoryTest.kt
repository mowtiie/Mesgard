package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.SettingsRemoteSource
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstNotificationSettingsRepositoryTest {
    private val remoteSource = mockk<SettingsRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns USER_ID }
    private val deviceSettings = FakeDeviceSettings()
    private val repository = OfflineFirstNotificationSettingsRepository(remoteSource, deviceSettings, authRepository)

    @Test
    fun `refresh uses the value the user chose`() = runTest {
        coEvery { remoteSource.fetchUserPreview(USER_ID) } returns "hidden"
        coEvery { remoteSource.fetchOrganizationPreviewDefault() } returns "sender_and_text"

        repository.refresh()

        assertEquals(NotificationPreview.HIDDEN, deviceSettings.settings.value.notificationPreview)
    }

    @Test
    fun `refresh falls back to the organization default when the user has not chosen`() = runTest {
        coEvery { remoteSource.fetchUserPreview(USER_ID) } returns null
        coEvery { remoteSource.fetchOrganizationPreviewDefault() } returns "sender_and_text"

        repository.refresh()

        assertEquals(NotificationPreview.SENDER_AND_TEXT, deviceSettings.settings.value.notificationPreview)
    }

    @Test
    fun `refresh shows only the sender when neither the user nor the organization chose a mode`() = runTest {
        coEvery { remoteSource.fetchUserPreview(USER_ID) } returns null
        coEvery { remoteSource.fetchOrganizationPreviewDefault() } returns null

        repository.refresh()

        assertEquals(NotificationPreview.SENDER_ONLY, deviceSettings.settings.value.notificationPreview)
    }

    @Test
    fun `refresh keeps the cached mode when the server cannot be reached`() = runTest {
        deviceSettings.setNotificationPreview(NotificationPreview.HIDDEN)
        coEvery { remoteSource.fetchUserPreview(any()) } throws IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertEquals(NotificationPreview.HIDDEN, deviceSettings.settings.value.notificationPreview)
    }

    @Test
    fun `setting a mode shows it immediately and saves it for the user`() = runTest {
        coEvery { remoteSource.saveUserPreview(USER_ID, "hidden") } returns Unit

        val result = repository.setPreview(NotificationPreview.HIDDEN)

        assertEquals(RefreshResult.Success, result)
        assertEquals(NotificationPreview.HIDDEN, deviceSettings.settings.value.notificationPreview)
        coVerify(exactly = 1) { remoteSource.saveUserPreview(USER_ID, "hidden") }
    }

    @Test
    fun `setting a mode restores the previous mode when saving fails`() = runTest {
        deviceSettings.setNotificationPreview(NotificationPreview.SENDER_ONLY)
        coEvery { remoteSource.saveUserPreview(any(), any()) } throws IOException("offline")

        val result = repository.setPreview(NotificationPreview.HIDDEN)

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertEquals(NotificationPreview.SENDER_ONLY, deviceSettings.settings.value.notificationPreview)
    }

    @Test
    fun `an unknown server value is ignored`() = runTest {
        coEvery { remoteSource.fetchUserPreview(USER_ID) } returns "everything"
        coEvery { remoteSource.fetchOrganizationPreviewDefault() } returns "hidden"

        repository.refresh()

        assertEquals(NotificationPreview.HIDDEN, deviceSettings.settings.value.notificationPreview)
    }

    private companion object {
        const val USER_ID = "me"
    }
}
