package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationType

/**
 * The title when the server gives none. A direct conversation has no title only when the other person has left the
 * organization, because the server names it after them.
 */
@StringRes
internal fun ConversationType.fallbackTitleResource(): Int = when (this) {
    ConversationType.DIRECT -> R.string.conversation_title_former_member
    ConversationType.GROUP -> R.string.conversation_title_group
    ConversationType.ANNOUNCEMENT -> R.string.conversation_title_announcement
}

/** Why the composer is missing: announcements are for staff, and a direct conversation needs two people. */
@StringRes
internal fun ConversationType?.readOnlyMessageResource(): Int =
    if (this == ConversationType.DIRECT) R.string.direct_read_only else R.string.announcement_read_only
