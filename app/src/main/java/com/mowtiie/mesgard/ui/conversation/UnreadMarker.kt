package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message

/** Where the unread messages begin when a conversation is opened. [count] is the unread count at that moment. */
internal data class UnreadMarker(val firstUnreadMessageId: String, val count: Int)

/**
 * The oldest of the [unreadCount] newest messages from other people. [messages] are newest first. When fewer
 * messages are loaded than are unread, the oldest loaded message from someone else is used.
 */
internal fun firstUnreadMessageId(messages: List<Message>, unreadCount: Int): String? {
    val incoming = messages.filter { !it.isOwn }
    if (unreadCount <= 0 || incoming.isEmpty()) return null
    return incoming[minOf(unreadCount, incoming.size) - 1].id
}

@Composable
internal fun UnreadDivider(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
        Text(
            text = pluralStringResource(R.plurals.conversation_unread_messages, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
    }
}
