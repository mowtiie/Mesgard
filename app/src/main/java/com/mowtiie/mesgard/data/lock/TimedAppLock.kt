package com.mowtiie.mesgard.data.lock

import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class TimedAppLock @Inject constructor(
    private val deviceSettings: DeviceSettingsRepository,
    private val clock: Clock,
) : AppLock {
    private val mutableIsLocked = MutableStateFlow(deviceSettings.settings.value.appLockEnabled)
    private var backgroundedAtMillis: Long? = null

    override val isLocked: StateFlow<Boolean> = mutableIsLocked.asStateFlow()

    override fun onAppBackgrounded() {
        backgroundedAtMillis = clock.millis()
    }

    override fun onAppForegrounded() {
        val backgroundedAt = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        val settings = deviceSettings.settings.value
        if (settings.appLockEnabled && clock.millis() - backgroundedAt >= settings.lockTimeout.milliseconds) {
            mutableIsLocked.value = true
        }
    }

    override fun unlock() {
        mutableIsLocked.value = false
    }
}
