package com.mowtiie.mesgard.data.attachment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val IMAGE_QUALITY = 90
private const val QUARTER_TURN_DEGREES = 90f
private const val HALF_TURN_DEGREES = 180f
private const val THREE_QUARTER_TURN_DEGREES = 270f

/**
 * Decodes a photo, applies its EXIF rotation, scales it down and encodes it again. The new file has no metadata,
 * so the location and the camera details in the original never leave the device.
 */
internal class ImageReencoder @Inject constructor(@ApplicationContext private val context: Context) {
    fun reencode(source: Uri, mimeType: String, target: File) {
        val bitmap = decodeUpright(source) ?: throw IOException("Cannot decode $source")
        try {
            val written = target.outputStream().use { bitmap.compress(compressFormat(mimeType), IMAGE_QUALITY, it) }
            if (!written) throw IOException("Cannot encode $source")
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeUpright(source: Uri): Bitmap? {
        val decoded = decodeSampled(source) ?: return null
        val (width, height) = scaledSize(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            postScale(width.toFloat() / decoded.width, height.toFloat() / decoded.height)
            postRotate(readRotationDegrees(source))
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    private fun decodeSampled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        return context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun readRotationDegrees(source: Uri): Float {
        val orientation = context.contentResolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN_DEGREES
            ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN_DEGREES
            ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN_DEGREES
            else -> 0f
        }
    }

    @Suppress("DEPRECATION")
    private fun compressFormat(mimeType: String) = when (mimeType) {
        "image/png" -> Bitmap.CompressFormat.PNG
        "image/webp" ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
        else -> Bitmap.CompressFormat.JPEG
    }
}
