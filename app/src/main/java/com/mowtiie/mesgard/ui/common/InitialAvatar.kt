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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AvatarSize = 48.dp
private const val AVATAR_COLOR_COUNT = 3

/** Picks one of the theme container colors from the name, so the same person always has the same color. */
internal fun avatarColorIndex(name: String): Int = Math.floorMod(name.trim().lowercase().hashCode(), AVATAR_COLOR_COUNT)

@Composable
internal fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = AvatarSize) {
    val (container, content) = avatarColors(avatarColorIndex(name))
    Surface(shape = CircleShape, color = container, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase().orEmpty(),
                style = if (size > AvatarSize) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = content,
            )
        }
    }
}

@Composable
private fun avatarColors(index: Int): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (index) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
}
