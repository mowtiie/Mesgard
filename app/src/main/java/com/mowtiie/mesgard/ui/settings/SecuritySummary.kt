package com.mowtiie.mesgard.ui.settings

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.DeviceSettings

/** The line under Privacy and security on the Settings home: what is turned on. */
internal enum class SecuritySummary(@StringRes val text: Int) {
    APP_LOCK_AND_SCREEN_PRIVACY(R.string.settings_security_summary_both),
    APP_LOCK(R.string.settings_security_summary_app_lock),
    SCREEN_PRIVACY(R.string.settings_security_summary_screen_privacy),
    NOTHING(R.string.settings_security_summary_nothing),
}

internal fun securitySummary(settings: DeviceSettings): SecuritySummary = when {
    settings.appLockEnabled && settings.screenPrivacyEnabled -> SecuritySummary.APP_LOCK_AND_SCREEN_PRIVACY
    settings.appLockEnabled -> SecuritySummary.APP_LOCK
    settings.screenPrivacyEnabled -> SecuritySummary.SCREEN_PRIVACY
    else -> SecuritySummary.NOTHING
}
