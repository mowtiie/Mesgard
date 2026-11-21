package com.mowtiie.mesgard.domain.model

import java.time.LocalTime

internal const val DEFAULT_QUIET_START_HOUR = 21
internal const val DEFAULT_QUIET_END_HOUR = 6

/**
 * What an organization restricts about messaging. Staff and students cannot message each other directly when
 * [blockStaffStudentDirectMessages] is on, and during quiet hours only organization admins can send. A window that
 * ends before it starts runs through midnight.
 */
internal data class MessagingRules(
    val blockStaffStudentDirectMessages: Boolean = false,
    val quietStart: LocalTime? = null,
    val quietEnd: LocalTime? = null,
    val timeZone: String = "Asia/Manila",
) {
    val hasQuietHours: Boolean
        get() = quietStart != null && quietEnd != null

    /** The window can be saved: both ends are chosen and they differ, or there is no window. */
    val isValid: Boolean
        get() = (quietStart == null && quietEnd == null) ||
            (quietStart != null && quietEnd != null && quietStart != quietEnd)
}
