package com.mowtiie.mesgard.domain.avatar

import org.junit.Assert.assertEquals
import org.junit.Test

class CropSquareTest {
    private val photo = PreparedPhoto("photo.jpg", width = 2000, height = 1000)
    private val viewport = 300f

    @Test
    fun `the photo covers the window with its shorter side`() {
        assertEquals(0.3f, coveringScale(photo, viewport), 0.0001f)
    }

    @Test
    fun `without zoom or move the window shows the middle square`() {
        val crop = cropSquare(photo, viewport, zoom = 1f, offsetX = 0f, offsetY = 0f)

        assertEquals(CropSquare(left = 500f, top = 0f, side = 1000f), crop.rounded())
    }

    @Test
    fun `zooming in shows a smaller square around the same center`() {
        val crop = cropSquare(photo, viewport, zoom = 2f, offsetX = 0f, offsetY = 0f)

        assertEquals(CropSquare(left = 750f, top = 250f, side = 500f), crop.rounded())
    }

    @Test
    fun `moving the photo to the right shows more of its left side`() {
        val crop = cropSquare(photo, viewport, zoom = 1f, offsetX = 90f, offsetY = 0f)

        assertEquals(200f, crop.left, 0.01f)
    }

    @Test
    fun `the photo can only move as far as its edge allows`() {
        assertEquals(150f, maxOffset(photo.width, coveringScale(photo, viewport), viewport), 0.01f)
        assertEquals(0f, maxOffset(photo.height, coveringScale(photo, viewport), viewport), 0.01f)
        assertEquals(150f, maxOffset(photo.height, coveringScale(photo, viewport) * 2, viewport), 0.01f)
    }

    @Test
    fun `at the farthest move the square stays inside the photo`() {
        val limit = maxOffset(photo.width, coveringScale(photo, viewport), viewport)

        val crop = cropSquare(photo, viewport, zoom = 1f, offsetX = limit, offsetY = 0f)

        assertEquals(0f, crop.left, 0.01f)
        assertEquals(photo.width.toFloat() - 1000f, cropSquare(photo, viewport, 1f, -limit, 0f).left, 0.01f)
    }

    private fun CropSquare.rounded() = CropSquare(left.roundTo(), top.roundTo(), side.roundTo())

    private fun Float.roundTo() = Math.round(this).toFloat()
}
