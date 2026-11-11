package com.mowtiie.mesgard.domain.model

private const val ONE_MINUTE_MS = 60_000L
private const val FIVE_MINUTES_MS = 5 * ONE_MINUTE_MS

internal enum class LockTimeout(val milliseconds: Long) {
    IMMEDIATELY(0L),
    ONE_MINUTE(ONE_MINUTE_MS),
    FIVE_MINUTES(FIVE_MINUTES_MS),
}

internal enum class NotificationPreview(val serverValue: String) {
    SENDER_AND_TEXT("sender_and_text"),
    SENDER_ONLY("sender_only"),
    HIDDEN("hidden"),
    ;

    companion object {
        fun fromServerValue(value: String?): NotificationPreview? = entries.firstOrNull { it.serverValue == value }
    }
}

internal enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

internal enum class ContrastLevel {
    STANDARD,
    MEDIUM,
    HIGH,
}

internal data class DeviceSettings(
    val screenPrivacyEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.ONE_MINUTE,
    val notificationPreview: NotificationPreview = NotificationPreview.SENDER_ONLY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val contrast: ContrastLevel = ContrastLevel.STANDARD,
    val dynamicColor: Boolean = false,
)
