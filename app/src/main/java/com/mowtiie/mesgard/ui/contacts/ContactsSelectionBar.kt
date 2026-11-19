package com.mowtiie.mesgard.ui.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.OverflowMenu

/** The contextual bar of a selection of people: Message for one person, Favorite for everyone, Select all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactsSelectionBar(
    state: ContactsSelectionUiState,
    callbacks: ContactsSelectionCallbacks,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = callbacks.onClose)
    val count = state.selected.size
    TopAppBar(
        modifier = modifier,
        title = { Text(pluralStringResource(R.plurals.selection_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = callbacks.onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.selection_close))
            }
        },
        actions = {
            state.single?.let { person ->
                IconButton(onClick = { callbacks.onMessage(person) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.conversation_start),
                    )
                }
            }
            FavoriteButton(removes = state.removesFavorites, onClick = callbacks.onFavorite)
            OverflowMenu { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.selection_select_all)) },
                    onClick = {
                        dismiss()
                        callbacks.onSelectAll()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** Filled once everyone selected is a favorite, so the icon shows the state the action leaves behind. */
@Composable
private fun FavoriteButton(removes: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (removes) {
            Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.contact_unfavorite))
        } else {
            Icon(
                painterResource(R.drawable.ic_star_outline),
                contentDescription = stringResource(R.string.contact_add_favorite),
            )
        }
    }
}
