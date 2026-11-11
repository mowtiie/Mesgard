package com.mowtiie.mesgard.ui.common

/** True when [text] contains the query, ignoring case. A blank query matches everything. */
internal fun matchesQuery(text: String, query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.isEmpty() || text.contains(trimmed, ignoreCase = true)
}
