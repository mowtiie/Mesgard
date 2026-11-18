package com.mowtiie.mesgard.domain.model

/** Someone in a conversation who can be mentioned in it. */
internal data class MentionCandidate(val userId: String, val displayName: String)
