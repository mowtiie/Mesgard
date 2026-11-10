package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationType

@StringRes
internal fun ConversationType.fallbackTitleResource(): Int = when (this) {
    ConversationType.DIRECT -> R.string.conversation_title_direct
    ConversationType.GROUP -> R.string.conversation_title_group
    ConversationType.ANNOUNCEMENT -> R.string.conversation_title_announcement
}
