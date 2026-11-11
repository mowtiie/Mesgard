package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimelineItemsTest {
    private val zone = ZoneOffset.UTC

    private fun message(id: String, year: Int, month: Int, day: Int, hour: Int = 12) = Message(
        id = id,
        conversationId = "c1",
        senderId = "s1",
        senderName = "Ana",
        body = "Body of $id",
        createdAtEpochMillis = ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli(),
        isEdited = false,
        isDeleted = false,
        status = MessageStatus.SENT,
        isOwn = false,
    )

    private fun List<TimelineItem>.describe() = map {
        when (it) {
            is TimelineItem.Bubble -> it.message.id
            is TimelineItem.DateHeader -> "header ${it.date}"
        }
    }

    @Test
    fun `an empty conversation has no items`() {
        assertTrue(emptyList<Message>().withDateHeaders(zone).isEmpty())
    }

    @Test
    fun `a single message is followed by the header of its day`() {
        val items = listOf(message("a", 2026, 11, 10)).withDateHeaders(zone)

        assertEquals(listOf("a", "header 2026-11-10"), items.describe())
    }

    @Test
    fun `messages of one day share a single header after the oldest message`() {
        val newestFirst = listOf(
            message("c", 2026, 11, 10, hour = 18),
            message("b", 2026, 11, 10, hour = 12),
            message("a", 2026, 11, 10, hour = 8),
        )

        assertEquals(listOf("c", "b", "a", "header 2026-11-10"), newestFirst.withDateHeaders(zone).describe())
    }

    @Test
    fun `each day gets its own header`() {
        val newestFirst = listOf(
            message("d", 2026, 11, 10),
            message("c", 2026, 11, 9, hour = 20),
            message("b", 2026, 11, 9, hour = 8),
            message("a", 2025, 12, 31),
        )

        assertEquals(
            listOf("d", "header 2026-11-10", "c", "b", "header 2026-11-09", "a", "header 2025-12-31"),
            newestFirst.withDateHeaders(zone).describe(),
        )
    }

    @Test
    fun `a day boundary follows the time zone of the device`() {
        val lateUtc = message("a", 2026, 11, 10, hour = 23)

        val items = listOf(lateUtc).withDateHeaders(ZoneOffset.ofHours(8))

        assertEquals(listOf("a", "header 2026-11-11"), items.describe())
    }

    @Test
    fun `keys are unique and stable`() {
        val newestFirst = listOf(message("b", 2026, 11, 10), message("a", 2026, 11, 9))

        val keys = newestFirst.withDateHeaders(zone).map { it.key }

        assertEquals(listOf("message:b", "date:2026-11-10", "message:a", "date:2026-11-09"), keys)
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `classifies today, yesterday and other days`() {
        val today = LocalDate.of(2026, 11, 10)

        assertEquals(RelativeDay.TODAY, relativeDay(today, today))
        assertEquals(RelativeDay.YESTERDAY, relativeDay(LocalDate.of(2026, 11, 9), today))
        assertEquals(RelativeDay.OTHER, relativeDay(LocalDate.of(2026, 11, 8), today))
        assertEquals(RelativeDay.YESTERDAY, relativeDay(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `a date in the current year omits the year`() {
        val text = formatDateHeader(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 10), Locale.US)

        assertEquals("Mon, Nov 2", text)
    }

    @Test
    fun `a date in another year includes the year`() {
        val text = formatDateHeader(LocalDate.of(2025, 10, 7), LocalDate.of(2026, 11, 10), Locale.US)

        assertEquals("Oct 7, 2025", text)
    }

    @Test
    fun `the message time shows only the time of day`() {
        val millis = ZonedDateTime.of(2026, 11, 10, 14, 5, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals("2:05 PM", formatMessageTime(millis, zone, Locale.US).replace(' ', ' '))
    }
}
