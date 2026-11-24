package com.mowtiie.mesgard.data.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.mowtiie.mesgard.domain.avatar.AVATAR_SIDE_PIXELS
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val AVATAR_WORKING_DIRECTORY = "avatar_working"
private const val WORKING_SIDE_PIXELS = 1536
private const val WORKING_QUALITY = 90
private const val PICTURE_QUALITY = 85
private const val QUARTER_TURN_DEGREES = 90f
private const val HALF_TURN_DEGREES = 180f
private const val THREE_QUARTER_TURN_DEGREES = 270f

/**
 * Makes the profile picture from a photo: the photo is turned upright and shrunk into a working copy with no metadata,
 * so the location and the camera details never leave the phone, and the crop is cut from that copy.
 */
internal class BitmapAvatarPhotos @Inject constructor(@ApplicationContext private val context: Context) :
    AvatarPhotos {
    override suspend fun prepare(uri: String): PreparedPhoto? = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val bitmap = decodeUpright(source) ?: return@withContext null
        try {
            val file = File(workingDirectory(), "photo-${System.nanoTime()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, WORKING_QUALITY, it) }
            PreparedPhoto(file.path, bitmap.width, bitmap.height)
        } catch (_: IOException) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    override suspend fun crop(photo: PreparedPhoto, crop: CropSquare): String? = withContext(Dispatchers.IO) {
        val whole = BitmapFactory.decodeFile(photo.path) ?: return@withContext null
        try {
            val side = crop.side.roundToInt().coerceIn(1, minOf(whole.width, whole.height))
            val left = crop.left.roundToInt().coerceIn(0, whole.width - side)
            val top = crop.top.roundToInt().coerceIn(0, whole.height - side)
            val square = Bitmap.createBitmap(whole, left, top, side, side)
            val scaled = Bitmap.createScaledBitmap(square, AVATAR_SIDE_PIXELS, AVATAR_SIDE_PIXELS, true)
            val file = File(workingDirectory(), "picture-${System.nanoTime()}.jpg")
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, PICTURE_QUALITY, it) }
            file.path
        } catch (_: IOException) {
            null
        } finally {
            whole.recycle()
        }
    }

    override fun discard(photo: PreparedPhoto, croppedPath: String?) {
        File(photo.path).delete()
        croppedPath?.let { File(it).delete() }
    }

    private fun decodeUpright(source: Uri): Bitmap? {
        val decoded = decodeShrunk(source) ?: return null
        val matrix = Matrix().apply { postRotate(readRotationDegrees(source)) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    private fun decodeShrunk(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val isReadable = bounds.outWidth > 0 && bounds.outHeight > 0
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight))
        }
        return if (isReadable) {
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
        } else {
            null
        }
    }

    private fun sampleSizeFor(longSide: Int): Int {
        var sample = 1
        while (longSide / (sample * 2) >= WORKING_SIDE_PIXELS) sample *= 2
        return sample
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

    private fun workingDirectory(): File = File(context.cacheDir, AVATAR_WORKING_DIRECTORY).also { it.mkdirs() }
}
