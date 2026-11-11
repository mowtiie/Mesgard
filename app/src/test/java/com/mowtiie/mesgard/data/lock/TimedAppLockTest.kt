package com.mowtiie.mesgard.data.lock

import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedAppLockTest {
    private val clock = MutableClock()

    @Test
    fun `starts locked when app lock is enabled`() {
        assertTrue(appLock(enabled = true).isLocked.value)
    }

    @Test
    fun `starts unlocked when app lock is disabled`() {
        assertFalse(appLock(enabled = false).isLocked.value)
    }

    @Test
    fun `stays unlocked when the app returns before the timeout`() {
        val appLock = unlockedAppLock(LockTimeout.ONE_MINUTE)

        appLock.onAppBackgrounded()
        clock.advanceMillis(LockTimeout.ONE_MINUTE.milliseconds - 1)
        appLock.onAppForegrounded()

        assertFalse(appLock.isLocked.value)
    }

    @Test
    fun `locks when the app returns after the timeout`() {
        val appLock = unlockedAppLock(LockTimeout.ONE_MINUTE)

        appLock.onAppBackgrounded()
        clock.advanceMillis(LockTimeout.ONE_MINUTE.milliseconds)
        appLock.onAppForegrounded()

        assertTrue(appLock.isLocked.value)
    }

    @Test
    fun `locks on every return when the timeout is immediate`() {
        val appLock = unlockedAppLock(LockTimeout.IMMEDIATELY)

        appLock.onAppBackgrounded()
        appLock.onAppForegrounded()

        assertTrue(appLock.isLocked.value)
    }

    @Test
    fun `does not lock when app lock is disabled`() {
        val appLock = appLock(enabled = false, timeout = LockTimeout.IMMEDIATELY)

        appLock.onAppBackgrounded()
        appLock.onAppForegrounded()

        assertFalse(appLock.isLocked.value)
    }

    @Test
    fun `ignores a foreground event that has no matching background event`() {
        val appLock = unlockedAppLock(LockTimeout.IMMEDIATELY)

        appLock.onAppForegrounded()

        assertFalse(appLock.isLocked.value)
    }

    @Test
    fun `unlock clears the lock`() {
        val appLock = appLock(enabled = true)

        appLock.unlock()

        assertFalse(appLock.isLocked.value)
    }

    private fun appLock(enabled: Boolean, timeout: LockTimeout = LockTimeout.ONE_MINUTE) =
        TimedAppLock(FakeDeviceSettings(DeviceSettings(appLockEnabled = enabled, lockTimeout = timeout)), clock)

    private fun unlockedAppLock(timeout: LockTimeout) = appLock(enabled = true, timeout = timeout).also { it.unlock() }

    private class MutableClock : Clock() {
        private var millis = START_MILLIS

        fun advanceMillis(amount: Long) {
            millis += amount
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(millis)

        override fun millis(): Long = millis

        private companion object {
            const val START_MILLIS = 1_700_000_000_000L
        }
    }
}
