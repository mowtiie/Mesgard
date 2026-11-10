package com.mowtiie.mesgard.ui.chats

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatActivityTime(
    activityEpochMillis: Long,
    nowEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val activity = Instant.ofEpochMilli(activityEpochMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
    val style = if (activity.toLocalDate() == now.toLocalDate()) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    }
    return style.withLocale(locale).format(activity)
}
