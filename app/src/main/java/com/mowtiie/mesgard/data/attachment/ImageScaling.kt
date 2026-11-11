package com.mowtiie.mesgard.data.attachment

import kotlin.math.roundToInt

internal const val MAX_IMAGE_EDGE_PX = 2048

/** The size an image of [width] by [height] is scaled to so its longest edge is at most [maxEdge]. */
internal fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_IMAGE_EDGE_PX): Pair<Int, Int> {
    val longest = maxOf(width, height)
    if (longest <= maxEdge) return width to height
    val ratio = maxEdge.toDouble() / longest
    return maxOf(1, (width * ratio).roundToInt()) to maxOf(1, (height * ratio).roundToInt())
}

/** The largest power of two that keeps the decoded image at least [maxEdge] on its longest edge. */
internal fun sampleSizeFor(width: Int, height: Int, maxEdge: Int = MAX_IMAGE_EDGE_PX): Int {
    var sampleSize = 1
    while (maxOf(width, height) / (sampleSize * 2) >= maxEdge) sampleSize *= 2
    return sampleSize
}
