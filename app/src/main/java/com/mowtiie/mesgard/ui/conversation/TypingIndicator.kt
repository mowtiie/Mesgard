package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

private const val MAX_NAMED_TYPERS = 2

/** The line shown under the conversation title while other people type. */
@Composable
internal fun typingText(names: List<String>): String = when {
    names.size > MAX_NAMED_TYPERS -> stringResource(R.string.typing_several)
    names.size == MAX_NAMED_TYPERS -> stringResource(R.string.typing_two, names[0], names[1])
    else -> stringResource(R.string.typing_one, names.first())
}
