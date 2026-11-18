package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MentionTextTest {
    @Test
    fun `an @ at the end of the draft starts a mention`() {
        assertEquals("", mentionQuery("@"))
        assertEquals("An", mentionQuery("Hello @An"))
        assertEquals("O'Neil", mentionQuery("Hi @O'Neil"))
    }

    @Test
    fun `an @ inside a word or followed by a space is not a mention`() {
        assertNull(mentionQuery("ana@school.example"))
        assertNull(mentionQuery("Hello @Ana Cruz"))
        assertNull(mentionQuery("Hello"))
        assertNull(mentionQuery(""))
    }

    @Test
    fun `choosing a name replaces what was typed after the @`() {
        assertEquals("Hello @Ana Cruz ", insertMention("Hello @An", "Ana Cruz"))
        assertEquals("@Ana Cruz ", insertMention("@", "Ana Cruz"))
        assertEquals("No mention here", insertMention("No mention here", "Ana Cruz"))
    }

    @Test
    fun `only the people whose name is still in the text are mentioned`() {
        val picks = linkedMapOf("u1" to "Ana Cruz", "u2" to "Ben Reyes")

        assertEquals(listOf("u1"), mentionedIdsIn("Hello @Ana Cruz, welcome", picks))
        assertEquals(emptyList<String>(), mentionedIdsIn("Hello everyone", picks))
    }

    @Test
    fun `a mention is shown in bold wherever the name appears`() {
        val text = linkedText("@Ana Cruz and again @ana cruz", listOf("Ana Cruz"))

        assertEquals(2, text.spanStyles.count { it.item.fontWeight == FontWeight.Bold })
        assertEquals(0, text.spanStyles.first().start)
    }
}
