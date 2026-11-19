package com.mowtiie.mesgard.ui.chats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.ui.conversation.typingText

/** A direct conversation only says typing, because its title already names the person; a group says who. */
@Composable
internal fun typingPreview(type: ConversationType, names: List<String>): String =
    if (type == ConversationType.DIRECT) stringResource(R.string.typing_short) else typingText(names)
