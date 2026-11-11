package com.mowtiie.mesgard.domain.model

private const val ONE_MINUTE_MS = 60_000L
private const val FIVE_MINUTES_MS = 5 * ONE_MINUTE_MS

internal enum class LockTimeout(val milliseconds: Long) {
    IMMEDIATELY(0L),
    ONE_MINUTE(ONE_MINUTE_MS),
    FIVE_MINUTES(FIVE_MINUTES_MS),
}

internal data class DeviceSettings(
    val screenPrivacyEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.ONE_MINUTE,
)
