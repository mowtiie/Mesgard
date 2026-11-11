package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateFormat
import com.mowtiie.mesgard.domain.model.Message
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val SAME_YEAR_SKELETON = "EEEMMMd"
private const val OTHER_YEAR_SKELETON = "yMMMd"

internal sealed interface TimelineItem {
    val key: String

    data class Bubble(val message: Message) : TimelineItem {
        override val key = "message:${message.id}"
    }

    data class DateHeader(val date: LocalDate) : TimelineItem {
        override val key = "date:$date"
    }
}

internal enum class RelativeDay {
    TODAY,
    YESTERDAY,
    OTHER,
}

internal fun relativeDay(date: LocalDate, today: LocalDate): RelativeDay = when (date) {
    today -> RelativeDay.TODAY
    today.minusDays(1) -> RelativeDay.YESTERDAY
    else -> RelativeDay.OTHER
}

/**
 * Takes messages newest first, as the conversation list shows them, and puts a date header after the oldest message
 * of each day. The list is reversed, so the header appears above that message on screen.
 */
internal fun List<Message>.withDateHeaders(zone: ZoneId): List<TimelineItem> {
    val items = ArrayList<TimelineItem>(size + 1)
    forEachIndexed { index, message ->
        items += TimelineItem.Bubble(message)
        val day = message.dayIn(zone)
        val olderDay = getOrNull(index + 1)?.dayIn(zone)
        if (day != olderDay) items += TimelineItem.DateHeader(day)
    }
    return items
}

private fun Message.dayIn(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(createdAtEpochMillis).atZone(zone).toLocalDate()

/** The time of day shown in a message bubble. */
internal fun formatMessageTime(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
) = DateTimeFormatter.ofLocalizedTime(
    FormatStyle.SHORT,
).withLocale(locale).format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** A date such as "Mon, Nov 9", with the year only when it differs from the current year. */
internal fun formatDateHeader(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    val skeleton = if (date.year == today.year) SAME_YEAR_SKELETON else OTHER_YEAR_SKELETON
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}
