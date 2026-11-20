package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerZoomTest {
    private val container = Size(1080f, 2000f)

    @Test
    fun `a wide photo is fitted to the width and a tall one to the height`() {
        assertEquals(Size(1080f, 540f), fittedSize(2000, 1000, container))
        assertEquals(Size(1000f, 2000f), fittedSize(1000, 2000, container))
    }

    @Test
    fun `an unknown photo size fills the container`() {
        assertEquals(container, fittedSize(null, null, container))
        assertEquals(container, fittedSize(0, 100, container))
    }

    @Test
    fun `the photo cannot be dragged while it is smaller than the container`() {
        val displayed = Size(1080f, 540f)

        assertEquals(
            Offset.Zero,
            clampOffset(Offset(300f, 300f), zoom = 1f, displayed = displayed, container = container),
        )
    }

    @Test
    fun `the photo can be dragged only as far as its own edges`() {
        val displayed = Size(1080f, 540f)

        val clamped = clampOffset(Offset(9_999f, 9_999f), zoom = 3f, displayed = displayed, container = container)

        assertEquals(1080f, clamped.x, 0.001f)
        assertEquals(0f, clamped.y, 0.001f)
    }

    @Test
    fun `zooming in from the middle needs no translation`() {
        val center = Offset(540f, 1000f)

        assertEquals(Offset.Zero, offsetKeepingFocus(center, center, 1f, 2.5f, Offset.Zero))
    }

    @Test
    fun `a double tap keeps the tapped point where it is`() {
        val center = Offset(540f, 1000f)
        val tap = Offset(840f, 700f)

        val offset = offsetKeepingFocus(tap, center, 1f, 2.5f, Offset.Zero)

        // The point of the photo under the tap, after scaling around the center and translating, is still the tap.
        val after = center + (tap - center) * 2.5f + offset
        assertEquals(tap.x, after.x, 0.001f)
        assertEquals(tap.y, after.y, 0.001f)
    }

    @Test
    fun `zooming out again returns to the middle`() {
        val center = Offset(540f, 1000f)
        val tap = Offset(840f, 700f)
        val zoomed = offsetKeepingFocus(tap, center, 1f, 2.5f, Offset.Zero)

        val back = offsetKeepingFocus(tap, center, 2.5f, 1f, zoomed)

        assertEquals(0f, back.x, 0.001f)
        assertEquals(0f, back.y, 0.001f)
    }
}
