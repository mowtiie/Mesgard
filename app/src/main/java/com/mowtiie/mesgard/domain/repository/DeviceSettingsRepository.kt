package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import kotlinx.coroutines.flow.StateFlow

internal interface DeviceSettingsRepository {
    val settings: StateFlow<DeviceSettings>

    fun setScreenPrivacyEnabled(enabled: Boolean)

    fun setAppLockEnabled(enabled: Boolean)

    fun setLockTimeout(timeout: LockTimeout)

    fun setNotificationPreview(preview: NotificationPreview)
}
