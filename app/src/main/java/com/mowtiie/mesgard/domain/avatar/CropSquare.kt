package com.mowtiie.mesgard.domain.avatar

import kotlin.math.max
import kotlin.math.min

internal const val AVATAR_SIDE_PIXELS = 512
internal const val AVATAR_MAX_ZOOM = 5f

/** A square taken from a photo, in the pixels of the photo. */
internal data class CropSquare(val left: Float, val top: Float, val side: Float)

/** The photo to crop. It is upright and has no metadata, so what is drawn is what is cut. */
internal data class PreparedPhoto(val path: String, val width: Int, val height: Int)

/**
 * The scale at which the photo exactly covers a square [viewport] with its shorter side. The person zooms in from
 * there, so the square never shows anything outside the photo.
 */
internal fun coveringScale(photo: PreparedPhoto, viewport: Float): Float = viewport / min(photo.width, photo.height)

/** How far the photo can be moved from the center at [zoom] before the square would show an edge. */
internal fun maxOffset(imageSide: Int, scale: Float, viewport: Float): Float = max(
    0f,
    (imageSide * scale - viewport) / 2,
)

/**
 * The part of the photo the square [viewport] shows when the photo is drawn at [zoom] times its covering scale and
 * moved by ([offsetX], [offsetY]) pixels from the center.
 */
internal fun cropSquare(
    photo: PreparedPhoto,
    viewport: Float,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
): CropSquare {
    val scale = coveringScale(photo, viewport) * zoom
    val left = (photo.width * scale / 2 - offsetX - viewport / 2) / scale
    val top = (photo.height * scale / 2 - offsetY - viewport / 2) / scale
    return CropSquare(left, top, viewport / scale)
}
