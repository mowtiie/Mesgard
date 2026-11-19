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
}

/** Why the composer is missing: a group may limit posting to its moderators, and a direct one needs two people. */
@StringRes
internal fun ConversationType?.readOnlyMessageResource(): Int =
    if (this == ConversationType.DIRECT) R.string.direct_read_only else R.string.group_read_only
