package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

internal interface DeviceSettingsRepository {
    val settings: StateFlow<DeviceSettings>

    fun setScreenPrivacyEnabled(enabled: Boolean)

    fun setAppLockEnabled(enabled: Boolean)

    fun setLockTimeout(timeout: LockTimeout)

    fun setNotificationPreview(preview: NotificationPreview)

    fun setThemeMode(mode: ThemeMode)

    fun setContrast(contrast: ContrastLevel)

    fun setDynamicColor(enabled: Boolean)
}
