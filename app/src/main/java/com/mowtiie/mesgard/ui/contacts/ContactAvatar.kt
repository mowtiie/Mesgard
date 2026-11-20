package com.mowtiie.mesgard.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.ui.common.InitialAvatar

private val DotSize = 14.dp
private val DotRing = 2.dp

/**
 * The avatar of a person, with a dot at its corner while the person is online. The dot has a ring in the color of the
 * list behind it, so it reads on any avatar color. The row says "Online" to screen readers, so the dot has no label.
 */
@Composable
internal fun ContactAvatar(name: String, isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        InitialAvatar(name)
        if (isOnline) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .size(DotSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(DotRing)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}
