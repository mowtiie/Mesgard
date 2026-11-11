package com.mowtiie.mesgard.testing

import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.ThemeMode
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeDeviceSettings(initial: DeviceSettings = DeviceSettings()) : DeviceSettingsRepository {
    private val mutableSettings = MutableStateFlow(initial)

    override val settings: StateFlow<DeviceSettings> = mutableSettings

    override fun setScreenPrivacyEnabled(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(screenPrivacyEnabled = enabled)
    }

    override fun setAppLockEnabled(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(appLockEnabled = enabled)
    }

    override fun setLockTimeout(timeout: LockTimeout) {
        mutableSettings.value = mutableSettings.value.copy(lockTimeout = timeout)
    }

    override fun setNotificationPreview(preview: NotificationPreview) {
        mutableSettings.value = mutableSettings.value.copy(notificationPreview = preview)
    }

    override fun setThemeMode(mode: ThemeMode) {
        mutableSettings.value = mutableSettings.value.copy(themeMode = mode)
    }

    override fun setContrast(contrast: ContrastLevel) {
        mutableSettings.value = mutableSettings.value.copy(contrast = contrast)
    }

    override fun setDynamicColor(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(dynamicColor = enabled)
    }
}
