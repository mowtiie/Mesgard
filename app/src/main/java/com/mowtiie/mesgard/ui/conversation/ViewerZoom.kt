package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

private const val HALF = 2f

/** The size a photo has when it is fitted into the container, or the container when the photo size is unknown. */
internal fun fittedSize(imageWidth: Int?, imageHeight: Int?, container: Size): Size {
    val isKnown = (imageWidth ?: 0) > 0 && (imageHeight ?: 0) > 0
    if (!isKnown || imageWidth == null || imageHeight == null) return container
    val scale = min(container.width / imageWidth, container.height / imageHeight)
    return Size(imageWidth * scale, imageHeight * scale)
}

/** How far the photo can be dragged from the center: only as far as its own edges, never into the empty margins. */
internal fun clampOffset(offset: Offset, zoom: Float, displayed: Size, container: Size): Offset {
    val maxX = max(0f, (displayed.width * zoom - container.width) / HALF)
    val maxY = max(0f, (displayed.height * zoom - container.height) / HALF)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/**
 * The translation that keeps the point under [focus] where it is while the zoom changes, for a layer that scales
 * around the center of the container. Used for a double tap and for the middle of a pinch.
 */
internal fun offsetKeepingFocus(
    focus: Offset,
    center: Offset,
    oldZoom: Float,
    newZoom: Float,
    oldOffset: Offset,
): Offset {
    val fromCenter = focus - center
    return fromCenter - (fromCenter - oldOffset) * (newZoom / oldZoom)
}
