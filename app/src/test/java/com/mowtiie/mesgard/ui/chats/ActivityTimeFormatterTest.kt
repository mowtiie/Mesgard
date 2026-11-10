package com.mowtiie.mesgard.ui.chats

import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTimeFormatterTest {
    private val now = Instant.parse("2026-11-10T15:00:00Z").toEpochMilli()

    @Test
    fun `shows the time for activity today`() {
        val activity = Instant.parse("2026-11-10T14:30:00Z").toEpochMilli()

        val text = formatActivityTime(activity, now, ZoneOffset.UTC, Locale.US)

        assertTrue(text, text.startsWith("2:30"))
    }

    @Test
    fun `shows the date for activity on an earlier day`() {
        val activity = Instant.parse("2026-11-09T14:30:00Z").toEpochMilli()

        val text = formatActivityTime(activity, now, ZoneOffset.UTC, Locale.US)

        assertTrue(text, text.startsWith("11/9"))
    }

    @Test
    fun `uses the local time zone to decide whether activity is today`() {
        val activity = Instant.parse("2026-11-10T23:30:00Z").toEpochMilli()
        val nextDayLocalNow = Instant.parse("2026-11-11T00:30:00Z").toEpochMilli()

        val text = formatActivityTime(activity, nextDayLocalNow, ZoneOffset.UTC, Locale.US)

        assertTrue(text, text.startsWith("11/10"))
    }
}
