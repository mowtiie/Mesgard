package com.mowtiie.mesgard.ui.common

import com.mowtiie.mesgard.domain.model.Contact

/** True when the name or the username of the person contains the query. A leading at sign is ignored. */
internal fun Contact.matchesQuery(query: String): Boolean =
    matchesQuery(displayName, query) || matchesQuery(username, query.trim().removePrefix("@"))

/** True when [text] contains the query, ignoring case. A blank query matches everything. */
internal fun matchesQuery(text: String, query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.isEmpty() || text.contains(trimmed, ignoreCase = true)
}
