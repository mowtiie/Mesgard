package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.AvatarImage

/** Opens the page where the picture of the group is changed. */
@Composable
internal fun GroupPictureRow(uiState: GroupDetailUiState, onOpen: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = { AvatarImage(uiState.avatar, uiState.name) },
        headlineContent = { Text(stringResource(R.string.group_picture_row)) },
        supportingContent = {
            Text(
                stringResource(
                    if (uiState.avatar != null) R.string.group_picture_row_set else R.string.group_picture_row_none,
                ),
            )
        },
    )
}
