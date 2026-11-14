package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message

/** The list counts as scrolled up once the newest messages are further away than this many items. */
internal const val SCROLLED_UP_ITEM_COUNT = 2

private const val MAX_BADGE_COUNT = 99

/**
 * Counts messages from other people that are newer than [lastSeenId]. [messages] are newest first. When the last seen
 * message is not loaded (or there is none) nothing is counted, so the badge never shows a wrong number.
 */
internal fun unseenIncomingCount(messages: List<Message>, lastSeenId: String?): Int {
    val lastSeenIndex = messages.indexOfFirst { it.id == lastSeenId }
    if (lastSeenIndex <= 0) return 0
    return messages.take(lastSeenIndex).count { !it.isOwn }
}

@Composable
internal fun ScrollToLatestButton(unseenCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.scroll_to_latest)
    val description = if (unseenCount > 0) {
        "$label, " + pluralStringResource(R.plurals.conversation_new_messages, unseenCount, unseenCount)
    } else {
        label
    }
    SmallFloatingActionButton(onClick = onClick, modifier = modifier.semantics { contentDescription = description }) {
        BadgedBox(
            badge = {
                if (unseenCount > 0) {
                    Badge {
                        Text(if (unseenCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else unseenCount.toString())
                    }
                }
            },
        ) {
            Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
    }
}
