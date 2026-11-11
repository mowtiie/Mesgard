package com.mowtiie.mesgard.ui.settings

import com.mowtiie.mesgard.domain.model.DeviceSettings

internal enum class SettingsError {
    DEVICE_LOCK_REQUIRED,
    PREVIEW_NETWORK_UNAVAILABLE,
    PREVIEW_UNKNOWN,
}

internal data class SettingsUiState(val settings: DeviceSettings = DeviceSettings(), val error: SettingsError? = null)
