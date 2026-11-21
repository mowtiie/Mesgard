package com.mowtiie.mesgard.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.mowtiie.mesgard.R

/** Trailing icon of a row that opens another page of the app. */
@Composable
internal fun NavigationChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Trailing icon of a row that opens a web page outside the app. */
@Composable
internal fun ExternalLinkIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_open_in_new),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Trailing icon of a row that copies its value when tapped. */
@Composable
internal fun CopyIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_content_copy),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
