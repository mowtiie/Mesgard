package com.mowtiie.mesgard.ui.profile.picture

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.domain.avatar.AVATAR_MAX_ZOOM
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.avatar.coveringScale
import com.mowtiie.mesgard.domain.avatar.cropSquare
import com.mowtiie.mesgard.domain.avatar.maxOffset
import java.io.File

private const val SHADE_ALPHA = 0.6f

/**
 * A square window onto the photo with a circle showing what stays. The person moves the photo with one finger and
 * zooms with two. The photo never leaves the window empty, because the zoom starts at the scale that covers it.
 * [onCropChanged] gets the part of the photo the window shows, in the pixels of the photo.
 */
@Composable
internal fun CropEditor(
    photo: PreparedPhoto,
    side: Dp,
    description: String,
    onCropChanged: (CropSquare) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewport = with(LocalDensity.current) { side.toPx() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scale = coveringScale(photo, viewport) * zoom
    val shade = MaterialTheme.colorScheme.scrim.copy(alpha = SHADE_ALPHA)

    LaunchedEffect(photo, viewport) { onCropChanged(cropSquare(photo, viewport, 1f, 0f, 0f)) }

    fun move(newZoom: Float, pan: Offset) {
        val limitX = maxOffset(photo.width, coveringScale(photo, viewport) * newZoom, viewport)
        val limitY = maxOffset(photo.height, coveringScale(photo, viewport) * newZoom, viewport)
        zoom = newZoom
        offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
        onCropChanged(cropSquare(photo, viewport, zoom, offset.x, offset.y))
    }

    Box(
        modifier = modifier
            .size(side)
            .clipToBounds()
            .semantics { contentDescription = description }
            .pointerInput(photo) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    move((zoom * zoomChange).coerceIn(1f, AVATAR_MAX_ZOOM), pan)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val imageWidth = with(LocalDensity.current) { (photo.width * scale / zoom).toDp() }
        val imageHeight = with(LocalDensity.current) { (photo.height * scale / zoom).toDp() }
        AsyncImage(
            model = File(photo.path),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .requiredSize(imageWidth, imageHeight)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offset.x
                    translationY = offset.y
                },
        )
        Box(
            modifier = Modifier
                .size(side)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawRect(shade)
                    drawCircle(Color.Transparent, radius = size.minDimension / 2, blendMode = BlendMode.Clear)
                },
        )
    }
}
