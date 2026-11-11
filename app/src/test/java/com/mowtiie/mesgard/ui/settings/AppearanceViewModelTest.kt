package com.mowtiie.mesgard.ui.settings

import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.ThemeMode
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceViewModelTest {
    private val deviceSettings = FakeDeviceSettings()
    private val viewModel = AppearanceViewModel(deviceSettings)

    @Test
    fun `exposes the device settings`() {
        assertEquals(ThemeMode.SYSTEM, viewModel.settings.value.themeMode)
    }

    @Test
    fun `changes the theme, the contrast and dynamic color`() {
        viewModel.onThemeModeSelected(ThemeMode.DARK)
        viewModel.onContrastSelected(ContrastLevel.HIGH)
        viewModel.onDynamicColorChanged(true)

        val settings = deviceSettings.settings.value
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(ContrastLevel.HIGH, settings.contrast)
        assertTrue(settings.dynamicColor)
    }
}
