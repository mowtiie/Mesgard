package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val UrlPattern = Regex("""https?://[^\s<>"]+""")
private const val TRAILING_PUNCTUATION = ".,;:!?)]}'"

/** Web addresses that start with http:// or https://, without the punctuation that ends a sentence. */
internal fun findUrls(text: String): List<IntRange> = UrlPattern.findAll(text).mapNotNull { match ->
    val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
    val hasHost = url.substringAfter("//").isNotEmpty()
    if (hasHost) match.range.first until match.range.first + url.length else null
}.toList()

private fun AnnotatedString.Builder.highlightAll(text: String, mention: String) {
    var start = text.indexOf(mention, ignoreCase = true)
    while (start >= 0) {
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + mention.length)
        start = text.indexOf(mention, start + mention.length, ignoreCase = true)
    }
}

/** The message text with every web address turned into a link that opens in the browser. */
internal fun linkedText(text: String, mentionNames: List<String> = emptyList()): AnnotatedString =
    buildAnnotatedString {
        append(text)
        mentionNames.forEach { name -> highlightAll(text, "@$name") }
        val style = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
        findUrls(text).forEach { range ->
            addLink(LinkAnnotation.Url(text.substring(range), style), range.first, range.last + 1)
        }
    }
