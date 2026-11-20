package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** The zoom and position of the photo on one page of the viewer. */
@Stable
internal class ZoomState {
    var zoom by mutableFloatStateOf(MIN_ZOOM)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var container = Size.Zero
    private var displayed = Size.Zero

    val isZoomed: Boolean get() = zoom > MIN_ZOOM

    private val center: Offset get() = Offset(container.width / HALF, container.height / HALF)

    fun onLaidOut(containerSize: Size, imageWidth: Int?, imageHeight: Int?) {
        container = containerSize
        displayed = fittedSize(imageWidth, imageHeight, containerSize)
    }

    /** A pinch or a drag. The photo keeps the point under the fingers where it is. */
    fun transform(centroid: Offset, pan: Offset, gestureZoom: Float) {
        val newZoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val keptFocus = offsetKeepingFocus(centroid, center, zoom, newZoom, offset)
        offset = clampOffset(keptFocus + pan, newZoom, displayed, container)
        zoom = newZoom
    }

    /** A double tap zooms in around the tap, or back out when the photo is already zoomed. */
    fun toggleAt(tap: Offset) {
        val newZoom = if (isZoomed) MIN_ZOOM else DOUBLE_TAP_ZOOM
        offset = clampOffset(offsetKeepingFocus(tap, center, zoom, newZoom, offset), newZoom, displayed, container)
        zoom = newZoom
    }
}
