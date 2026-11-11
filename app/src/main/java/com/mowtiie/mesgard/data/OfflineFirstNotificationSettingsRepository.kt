package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.SettingsRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import com.mowtiie.mesgard.domain.repository.NotificationSettingsRepository
import javax.inject.Inject

private const val TAG = "NotificationSettings"

internal class OfflineFirstNotificationSettingsRepository @Inject constructor(
    private val remoteSource: SettingsRemoteSource,
    private val deviceSettings: DeviceSettingsRepository,
    private val authRepository: AuthRepository,
) : NotificationSettingsRepository {
    override suspend fun refresh(): RefreshResult {
        val userId = authRepository.currentUserId ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        return attemptRemote(TAG) {
            val userValue = remoteSource.fetchUserPreview(userId)
            val organizationValue = remoteSource.fetchOrganizationPreviewDefault()
            val effective = NotificationPreview.fromServerValue(userValue)
                ?: NotificationPreview.fromServerValue(organizationValue)
                ?: NotificationPreview.SENDER_ONLY
            deviceSettings.setNotificationPreview(effective)
        }.toRefreshResult()
    }

    override suspend fun setPreview(preview: NotificationPreview): RefreshResult {
        val previous = deviceSettings.settings.value.notificationPreview
        deviceSettings.setNotificationPreview(preview)
        val outcome = attemptRemote(TAG) { remoteSource.saveUserPreview(preview.serverValue) }
        if (outcome is RemoteOutcome.Failure) deviceSettings.setNotificationPreview(previous)
        return outcome.toRefreshResult()
    }
}
