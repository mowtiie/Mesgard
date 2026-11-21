package com.mowtiie.mesgard.domain.model

private const val NOTICE_LINK_MAX_LENGTH = 500
private val NoticeLink = Regex("^https?://\\S+$")

/** The privacy notice an organization links, and whether the signed in person has acknowledged this version of it. */
internal data class PrivacyNotice(
    val orgId: String,
    val orgName: String,
    val noticeUrl: String?,
    val childNoticeUrl: String?,
    val isAcknowledged: Boolean,
) {
    val hasLinks: Boolean
        get() = noticeUrl != null || childNoticeUrl != null

    val needsAcknowledgment: Boolean
        get() = hasLinks && !isAcknowledged
}

/** A link is optional. When given it has to be an http or https address without spaces, as the server checks too. */
internal fun isValidNoticeLink(link: String): Boolean {
    val trimmed = link.trim()
    return trimmed.isEmpty() || (trimmed.length <= NOTICE_LINK_MAX_LENGTH && NoticeLink.matches(trimmed))
}
