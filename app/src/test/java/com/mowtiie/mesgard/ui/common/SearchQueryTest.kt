package com.mowtiie.mesgard.ui.common

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {
    @Test
    fun `matches a query inside the text and ignores case and surrounding spaces`() {
        assertTrue(matchesQuery("Ana Reyes", " rey "))
        assertTrue(matchesQuery("Ana Reyes", "ANA"))
    }

    @Test
    fun `a blank query matches everything`() {
        assertTrue(matchesQuery("Ana Reyes", ""))
        assertTrue(matchesQuery("Ana Reyes", "   "))
    }

    @Test
    fun `does not match text that does not contain the query`() {
        assertFalse(matchesQuery("Ana Reyes", "ben"))
    }

    private val person = Contact("org1", "u1", "Ana Reyes", OrgRole.MEMBER, username = "ana.reyes")

    @Test
    fun `a person matches by name or by username, with or without the at sign`() {
        assertTrue(person.matchesQuery("reyes"))
        assertTrue(person.matchesQuery("ana."))
        assertTrue(person.matchesQuery("@ana.re"))
        assertFalse(person.matchesQuery("@ben"))
    }

    @Test
    fun `a person without a username still matches by name`() {
        assertTrue(person.copy(username = "").matchesQuery("rey"))
        assertFalse(person.copy(username = "").matchesQuery("@ana"))
    }
}
