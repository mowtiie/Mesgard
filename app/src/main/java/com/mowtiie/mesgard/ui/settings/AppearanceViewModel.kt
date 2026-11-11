package com.mowtiie.mesgard.ui.settings

import androidx.lifecycle.ViewModel
import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.ThemeMode
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
internal class AppearanceViewModel @Inject constructor(private val deviceSettings: DeviceSettingsRepository) :
    ViewModel() {
    val settings: StateFlow<DeviceSettings> = deviceSettings.settings

    fun onThemeModeSelected(mode: ThemeMode) = deviceSettings.setThemeMode(mode)

    fun onContrastSelected(contrast: ContrastLevel) = deviceSettings.setContrast(contrast)

    fun onDynamicColorChanged(enabled: Boolean) = deviceSettings.setDynamicColor(enabled)
}
