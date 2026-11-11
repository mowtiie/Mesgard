package com.mowtiie.mesgard.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Search is a secondary action of a list, so it is an icon button in the app bar that opens [SearchAppBar]. */
@Composable
internal fun SearchAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
    }
}

/** A disabled item stays in the menu while the action is unavailable, as the Material menu guidelines ask. */
@Composable
internal fun RefreshMenuItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.refresh)) },
        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
        enabled = enabled,
        onClick = onClick,
    )
}
