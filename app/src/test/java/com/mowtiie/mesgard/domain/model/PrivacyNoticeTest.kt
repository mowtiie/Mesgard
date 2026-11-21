package com.mowtiie.mesgard.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyNoticeTest {
    private fun notice(url: String? = "https://school.example/privacy", child: String? = null, done: Boolean = false) =
        PrivacyNotice("org", "School", url, child, isAcknowledged = done)

    @Test
    fun `a notice that has not been acknowledged is due`() {
        assertTrue(notice().needsAcknowledgment)
        assertTrue(notice(url = null, child = "https://school.example/kids").needsAcknowledgment)
    }

    @Test
    fun `an acknowledged notice is not due again`() {
        assertFalse(notice(done = true).needsAcknowledgment)
    }

    @Test
    fun `an organization without a notice has nothing to acknowledge`() {
        val none = notice(url = null)

        assertFalse(none.hasLinks)
        assertFalse(none.needsAcknowledgment)
    }

    @Test
    fun `a link is empty or an http or https address without spaces`() {
        assertTrue(isValidNoticeLink(""))
        assertTrue(isValidNoticeLink("   "))
        assertTrue(isValidNoticeLink("https://school.example/privacy"))
        assertTrue(isValidNoticeLink(" http://intranet.local/kids "))
        assertFalse(isValidNoticeLink("javascript:alert(1)"))
        assertFalse(isValidNoticeLink("school.example/privacy"))
        assertFalse(isValidNoticeLink("https://school.example/a b"))
        assertFalse(isValidNoticeLink("ftp://school.example/privacy"))
    }

    @Test
    fun `a link that is too long is refused`() {
        assertFalse(isValidNoticeLink("https://school.example/" + "a".repeat(500)))
    }
}
