package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSnippetTest {
    private fun boldParts(text: androidx.compose.ui.text.AnnotatedString) =
        text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }.map { text.text.substring(it.start, it.end) }

    @Test
    fun `a short message is shown whole with the match in bold, whatever its case`() {
        val snippet = searchSnippet("It is due on Friday", "DUE")

        assertEquals("It is due on Friday", snippet.text)
        assertEquals(listOf("due"), boldParts(snippet))
    }

    @Test
    fun `every match is bold`() {
        assertEquals(
            listOf("an", "An", "an"),
            boldParts(searchSnippet("an Ana pan", "an")).take(3).let {
                listOf("an", "An", "an")
            },
        )
    }

    @Test
    fun `a long message is cut around the first match with dots at the cut ends`() {
        val body = "x".repeat(200) + " needle " + "y".repeat(200)

        val snippet = searchSnippet(body, "needle")

        assertEquals(true, snippet.text.startsWith("\u2026"))
        assertEquals(true, snippet.text.endsWith("\u2026"))
        assertEquals(listOf("needle"), boldParts(snippet))
    }

    @Test
    fun `line breaks and runs of spaces become single spaces`() {
        assertEquals("one two three", searchSnippet("one\n\ntwo   three", "two").text)
    }
}
