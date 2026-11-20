package com.mowtiie.mesgard.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Search is a secondary action of a list, so it is an icon button in the app bar that opens [SearchAppBar]. */
@Composable
internal fun SearchAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search))
    }
}
