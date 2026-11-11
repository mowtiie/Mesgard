package com.mowtiie.mesgard.ui.theme

import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test
    fun `picks the standard schemes for the standard contrast`() {
        assertEquals(LightColorScheme, colorSchemeFor(darkTheme = false, contrast = ContrastLevel.STANDARD))
        assertEquals(DarkColorScheme, colorSchemeFor(darkTheme = true, contrast = ContrastLevel.STANDARD))
    }

    @Test
    fun `picks the medium and high contrast schemes for each brightness`() {
        assertEquals(MediumContrastLightColorScheme, colorSchemeFor(false, ContrastLevel.MEDIUM))
        assertEquals(MediumContrastDarkColorScheme, colorSchemeFor(true, ContrastLevel.MEDIUM))
        assertEquals(HighContrastLightColorScheme, colorSchemeFor(false, ContrastLevel.HIGH))
        assertEquals(HighContrastDarkColorScheme, colorSchemeFor(true, ContrastLevel.HIGH))
    }

    @Test
    fun `every contrast level has its own colors`() {
        val primaries = ContrastLevel.entries.map { colorSchemeFor(false, it).primary }

        assertEquals(primaries.size, primaries.toSet().size)
        assertNotEquals(LightColorScheme.onSurface, HighContrastLightColorScheme.onSurface)
    }

    @Test
    fun `follows the system unless the mode is forced`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }
}
