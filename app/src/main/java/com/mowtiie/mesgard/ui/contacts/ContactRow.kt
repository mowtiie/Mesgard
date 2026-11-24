package com.mowtiie.mesgard.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.ui.common.labelResource

@Composable
internal fun ContactRow(
    contact: Contact,
    uiState: ContactsUiState,
    onContactClicked: (Contact) -> Unit,
    onFavoriteToggled: (Contact) -> Unit,
) {
    val isOnline = contact.userId in uiState.onlineUserIds
    val onlineLabel = stringResource(R.string.contact_online)
    ListItem(
        modifier = Modifier
            .clickable(
                enabled = !uiState.isStartingConversation,
                onClickLabel = stringResource(R.string.conversation_start),
            ) { onContactClicked(contact) }
            .semantics { if (isOnline) stateDescription = onlineLabel },
        leadingContent = { ContactAvatar(contact.displayName, isOnline) },
        headlineContent = { Text(contact.displayName) },
        supportingContent = { Text(stringResource(contact.role.labelResource())) },
        trailingContent = { FavoriteToggle(contact.isFavorite) { onFavoriteToggled(contact) } },
    )
}

/** Filled while the person is a favorite, which is the toggled on state, and outlined otherwise. */
@Composable
private fun FavoriteToggle(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (isFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.contact_unfavorite),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                Icons.Outlined.Star,
                contentDescription = stringResource(R.string.contact_add_favorite),
            )
        }
    }
}
