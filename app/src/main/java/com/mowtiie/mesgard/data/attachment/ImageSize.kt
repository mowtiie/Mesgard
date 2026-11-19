package com.mowtiie.mesgard.data.attachment

import android.graphics.BitmapFactory
import android.media.ExifInterface
import java.io.File
import java.io.IOException

/** The pixel size as shown, so a photo that the camera stored sideways is reported upright. Null for other files. */
internal fun readDisplaySize(file: File, mimeType: String): Pair<Int, Int>? {
    if (!mimeType.startsWith("image/")) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val hasSize = bounds.outWidth > 0 && bounds.outHeight > 0
    return when {
        !hasSize -> null
        isStoredSideways(file) -> bounds.outHeight to bounds.outWidth
        else -> bounds.outWidth to bounds.outHeight
    }
}

private fun isStoredSideways(file: File): Boolean = try {
    val orientation = ExifInterface(file.absolutePath)
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
} catch (_: IOException) {
    false
}
