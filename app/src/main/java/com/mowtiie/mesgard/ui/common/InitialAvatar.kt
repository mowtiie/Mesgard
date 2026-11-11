package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AvatarSize = 48.dp

@Composable
internal fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = AvatarSize) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase().orEmpty(),
                style = if (size >
                    AvatarSize
                ) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
