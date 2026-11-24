package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.data.attachment.StoredFile
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.AvatarSource
import kotlinx.coroutines.flow.flowOf

private val DefaultAvatarSize = 48.dp

/** Provided where the app is composed. Without it, every avatar shows the initials. */
internal val LocalAvatarSource = compositionLocalOf<AvatarSource?> { null }

/**
 * The profile picture of a person, or the initials while there is none, while it loads, and when it cannot be had,
 * for example because the organization turned pictures off. The picture is looked up from the roster on the phone.
 */
@Composable
internal fun AvatarImage(userId: String, name: String, modifier: Modifier = Modifier, size: Dp = DefaultAvatarSize) {
    val source = LocalAvatarSource.current
    val refs = remember(source, userId) { source?.observeRef(userId) ?: flowOf(null) }
    val ref by refs.collectAsStateWithLifecycle(initialValue = null)
    AvatarImage(ref, name, modifier, size)
}

/** The picture of a known [ref], for rows that already hold the version. A null [ref] shows the initials. */
@Composable
internal fun AvatarImage(ref: AvatarRef?, name: String, modifier: Modifier = Modifier, size: Dp = DefaultAvatarSize) {
    val source = LocalAvatarSource.current
    val path by produceState<String?>(initialValue = null, source, ref) {
        value = if (source != null && ref != null) source.localPath(ref) else null
    }
    val shown = path
    if (shown == null) {
        InitialAvatar(name, modifier, size)
    } else {
        AsyncImage(
            model = StoredFile(shown),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).clearAndSetSemantics {},
        )
    }
}
