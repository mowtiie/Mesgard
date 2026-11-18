package com.mowtiie.mesgard.domain.model

/** A message that matches a search in a conversation. */
internal data class SearchHit(
    val messageId: String,
    val senderName: String,
    val body: String,
    val createdAtEpochMillis: Long,
)

/** Where the next page of a search starts, with the server's own timestamp so no precision is lost. */
internal data class SearchCursor(val createdAt: String, val messageId: String)

internal data class SearchPage(val hits: List<SearchHit>, val next: SearchCursor?)

internal sealed interface SearchOutcome {
    data class Loaded(val page: SearchPage) : SearchOutcome

    data class Failure(val error: RefreshError) : SearchOutcome
}
