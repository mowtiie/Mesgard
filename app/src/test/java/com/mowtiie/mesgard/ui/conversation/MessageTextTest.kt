package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTextTest {
    @Test
    fun `finds an https address inside a sentence`() {
        val text = "See https://example.com/week-12 for details"

        assertEquals(listOf("https://example.com/week-12"), findUrls(text).map { text.substring(it) })
    }

    @Test
    fun `leaves out the punctuation that ends a sentence`() {
        val text = "Open (https://example.com/a), then https://example.com/b."

        assertEquals(
            listOf("https://example.com/a", "https://example.com/b"),
            findUrls(text).map { text.substring(it) },
        )
    }

    @Test
    fun `ignores text that is not a web address`() {
        assertTrue(findUrls("report.pdf, www.example.com and ftp://example.com").isEmpty())
        assertTrue(findUrls("https:// alone").isEmpty())
    }

    @Test
    fun `links every address and keeps the text unchanged`() {
        val text = "a https://example.com b http://example.org/x"

        val linked = linkedText(text)

        assertEquals(text, linked.text)
        val urls = linked.getLinkAnnotations(0, text.length).map { (it.item as LinkAnnotation.Url).url }
        assertEquals(listOf("https://example.com", "http://example.org/x"), urls)
    }
}
