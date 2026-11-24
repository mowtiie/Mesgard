package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Archived conversations leave the list, so one row at the top leads to them. */
@Composable
internal fun ArchivedRow(count: Int, onClick: () -> Unit) {
    ListItem(
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = { Text(stringResource(R.string.archived_title)) },
        trailingContent = { Text(formatUnreadCount(count), style = MaterialTheme.typography.labelMedium) },
    )
}
