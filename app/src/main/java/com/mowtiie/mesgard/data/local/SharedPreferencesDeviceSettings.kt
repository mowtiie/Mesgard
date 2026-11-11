package com.mowtiie.mesgard.data.local

import android.content.SharedPreferences
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val DEVICE_SETTINGS_FILE = "device_settings"
private const val KEY_SCREEN_PRIVACY = "screen_privacy_enabled"
private const val KEY_APP_LOCK = "app_lock_enabled"
private const val KEY_LOCK_TIMEOUT = "lock_timeout"
private const val KEY_NOTIFICATION_PREVIEW = "notification_preview"

internal class SharedPreferencesDeviceSettings(private val preferences: SharedPreferences) :
    DeviceSettingsRepository {
    private val mutableSettings = MutableStateFlow(read())

    override val settings: StateFlow<DeviceSettings> = mutableSettings.asStateFlow()

    override fun setScreenPrivacyEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SCREEN_PRIVACY, enabled).apply()
        mutableSettings.value = mutableSettings.value.copy(screenPrivacyEnabled = enabled)
    }

    override fun setAppLockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        mutableSettings.value = mutableSettings.value.copy(appLockEnabled = enabled)
    }

    override fun setLockTimeout(timeout: LockTimeout) {
        preferences.edit().putString(KEY_LOCK_TIMEOUT, timeout.name).apply()
        mutableSettings.value = mutableSettings.value.copy(lockTimeout = timeout)
    }

    override fun setNotificationPreview(preview: NotificationPreview) {
        preferences.edit().putString(KEY_NOTIFICATION_PREVIEW, preview.name).apply()
        mutableSettings.value = mutableSettings.value.copy(notificationPreview = preview)
    }

    private fun read(): DeviceSettings {
        val defaults = DeviceSettings()
        return DeviceSettings(
            screenPrivacyEnabled = preferences.getBoolean(KEY_SCREEN_PRIVACY, defaults.screenPrivacyEnabled),
            appLockEnabled = preferences.getBoolean(KEY_APP_LOCK, defaults.appLockEnabled),
            lockTimeout = preferences.getString(KEY_LOCK_TIMEOUT, null)
                ?.let { name -> LockTimeout.entries.firstOrNull { it.name == name } }
                ?: defaults.lockTimeout,
            notificationPreview = preferences.getString(KEY_NOTIFICATION_PREVIEW, null)
                ?.let { name -> NotificationPreview.entries.firstOrNull { it.name == name } }
                ?: defaults.notificationPreview,
        )
    }
}
