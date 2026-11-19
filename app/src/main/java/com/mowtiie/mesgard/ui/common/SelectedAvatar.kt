package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AvatarSize = 48.dp

/** Replaces the avatar of a row that is part of a selection. */
@Composable
internal fun SelectedAvatar(modifier: Modifier = Modifier, size: Dp = AvatarSize) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
