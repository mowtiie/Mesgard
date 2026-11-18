package com.mowtiie.mesgard.ui.conversation

private const val MENTION_MARK = '@'
private const val MAX_QUERY_LENGTH = 30
private val QueryPattern = Regex("""(?:^|\s)@([\p{L}\p{N}'.\-]{0,$MAX_QUERY_LENGTH})$""")

/**
 * What the person is typing after an @ at the end of the draft, or null when they are not typing a mention. The @ has
 * to start a word, so an email address is never taken for a mention.
 */
internal fun mentionQuery(draft: String): String? = QueryPattern.find(draft)?.groupValues?.get(1)

/** Replaces the @ and what follows it at the end of the draft with the chosen name and a space. */
internal fun insertMention(draft: String, displayName: String): String {
    val query = mentionQuery(draft) ?: return draft
    return draft.dropLast(query.length + 1) + MENTION_MARK + displayName + " "
}

/** The chosen people whose @name is still in the text, so a mention that was deleted again is not sent. */
internal fun mentionedIdsIn(draft: String, picks: Map<String, String>): List<String> =
    picks.filterValues { name -> draft.contains(MENTION_MARK + name) }.keys.toList()
