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
internal const val UNREAD_DIVIDER_KEY = "unread-divider"
private const val GROUP_WINDOW_MILLIS = 5 * 60 * 1000L

internal sealed interface TimelineItem {
    val key: String

    /** [startsGroup] and [endsGroup] mark the first and last message of a run from one sender. */
    data class Bubble(val message: Message, val startsGroup: Boolean = true, val endsGroup: Boolean = true) :
        TimelineItem {
        override val key = "message:${message.id}"
    }

    data class DateHeader(val date: LocalDate) : TimelineItem {
        override val key = "date:$date"
    }

    data class UnreadDivider(val count: Int) : TimelineItem {
        override val key = UNREAD_DIVIDER_KEY
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
 * of each day. The list is reversed, so the header appears above that message on screen. The unread divider goes
 * above the first unread message, between it and the date header.

 */
internal fun List<Message>.withDateHeaders(zone: ZoneId, unread: UnreadMarker? = null): List<TimelineItem> {
    val items = ArrayList<TimelineItem>(size + 1)
    forEachIndexed { index, message ->
        val older = getOrNull(index + 1)
        val newer = getOrNull(index - 1)
        items += TimelineItem.Bubble(
            message = message,
            startsGroup = !message.followsInGroup(older, zone),
            endsGroup = newer == null || !newer.followsInGroup(message, zone),
        )
        if (message.id == unread?.firstUnreadMessageId) items += TimelineItem.UnreadDivider(unread.count)
        val day = message.dayIn(zone)
        val olderDay = older?.dayIn(zone)
        if (day != olderDay) items += TimelineItem.DateHeader(day)
    }
    return items
}

/** True when this message continues a run of [previous]: same sender, same day and sent shortly after. */
private fun Message.followsInGroup(previous: Message?, zone: ZoneId): Boolean = previous != null &&
    senderId == previous.senderId &&
    dayIn(zone) == previous.dayIn(zone) &&
    createdAtEpochMillis - previous.createdAtEpochMillis < GROUP_WINDOW_MILLIS

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
