package com.mowtiie.mesgard.ui.settings

import com.mowtiie.mesgard.domain.model.DeviceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SecuritySummaryTest {
    @Test
    fun `names both protections when both are on`() {
        val settings = DeviceSettings(appLockEnabled = true, screenPrivacyEnabled = true)

        assertEquals(SecuritySummary.APP_LOCK_AND_SCREEN_PRIVACY, securitySummary(settings))
    }

    @Test
    fun `names the app lock when only it is on`() {
        val settings = DeviceSettings(appLockEnabled = true, screenPrivacyEnabled = false)

        assertEquals(SecuritySummary.APP_LOCK, securitySummary(settings))
    }

    @Test
    fun `names screen privacy when only it is on`() {
        val settings = DeviceSettings(appLockEnabled = false, screenPrivacyEnabled = true)

        assertEquals(SecuritySummary.SCREEN_PRIVACY, securitySummary(settings))
    }

    @Test
    fun `says nothing is on when both are off`() {
        val settings = DeviceSettings(appLockEnabled = false, screenPrivacyEnabled = false)

        assertEquals(SecuritySummary.NOTHING, securitySummary(settings))
    }
}
