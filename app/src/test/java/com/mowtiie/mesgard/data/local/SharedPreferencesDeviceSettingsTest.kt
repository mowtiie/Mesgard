package com.mowtiie.mesgard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.domain.model.LockTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesDeviceSettingsTest {
    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("test_device_settings", Context.MODE_PRIVATE)

    @Test
    fun `defaults to screen privacy on, app lock off and a one minute timeout`() {
        val settings = SharedPreferencesDeviceSettings(preferences).settings.value

        assertTrue(settings.screenPrivacyEnabled)
        assertFalse(settings.appLockEnabled)
        assertEquals(LockTimeout.ONE_MINUTE, settings.lockTimeout)
    }

    @Test
    fun `keeps the changes for a later instance`() {
        val first = SharedPreferencesDeviceSettings(preferences)
        first.setScreenPrivacyEnabled(false)
        first.setAppLockEnabled(true)
        first.setLockTimeout(LockTimeout.FIVE_MINUTES)

        val restored = SharedPreferencesDeviceSettings(preferences).settings.value

        assertFalse(restored.screenPrivacyEnabled)
        assertTrue(restored.appLockEnabled)
        assertEquals(LockTimeout.FIVE_MINUTES, restored.lockTimeout)
    }

    @Test
    fun `falls back to the default timeout when the stored value is unknown`() {
        preferences.edit().putString("lock_timeout", "TEN_MINUTES").commit()

        assertEquals(LockTimeout.ONE_MINUTE, SharedPreferencesDeviceSettings(preferences).settings.value.lockTimeout)
    }

    @Test
    fun `publishes a change to its observers`() {
        val settings = SharedPreferencesDeviceSettings(preferences)

        settings.setAppLockEnabled(true)

        assertTrue(settings.settings.value.appLockEnabled)
    }
}
