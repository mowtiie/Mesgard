package com.mowtiie.mesgard.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mowtiie.mesgard.R

/** The one Sign out row, in the error color so it reads as different from the rows that only open a page. */
@Composable
internal fun SignOutRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    ListItem(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(headlineColor = color, leadingIconColor = color),
        leadingContent = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.sign_out_button)) },
    )
}
