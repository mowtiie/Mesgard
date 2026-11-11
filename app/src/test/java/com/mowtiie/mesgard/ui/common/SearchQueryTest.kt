package com.mowtiie.mesgard.ui.common

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
}
