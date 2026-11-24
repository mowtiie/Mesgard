package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val IconPadding = 16.dp

/**
 * Swiping a message to the right replies to it. The message always settles back, because nothing is dismissed. The
 * Reply item of the long press menu does the same for people who cannot swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplySwipe(enabled: Boolean, onReply: () -> Unit, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onReply()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(start = IconPadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        content = { content() },
    )
}
