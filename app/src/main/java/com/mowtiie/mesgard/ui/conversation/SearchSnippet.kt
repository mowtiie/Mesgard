package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

private const val CONTEXT_BEFORE = 30
private const val ELLIPSIS = "\u2026"

/** A short stretch of the message around the first match, with every match in bold. */
internal fun searchSnippet(body: String, query: String, maxLength: Int = 120): AnnotatedString {
    val text = body.replace(Regex("\\s+"), " ").trim()
    val needle = query.trim()
    val first = if (needle.isEmpty()) -1 else text.indexOf(needle, ignoreCase = true)
    val start = if (first > CONTEXT_BEFORE) first - CONTEXT_BEFORE else 0
    val end = minOf(text.length, start + maxLength)
    val shown = text.substring(start, end)
    return buildAnnotatedString {
        if (start > 0) append(ELLIPSIS)
        val offset = length
        append(shown)
        if (end < text.length) append(ELLIPSIS)
        var at = if (needle.isEmpty()) -1 else shown.indexOf(needle, ignoreCase = true)
        while (at >= 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), offset + at, offset + at + needle.length)
            at = shown.indexOf(needle, at + needle.length, ignoreCase = true)
        }
    }
}
