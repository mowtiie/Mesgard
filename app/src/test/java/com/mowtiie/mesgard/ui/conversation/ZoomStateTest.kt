package com.mowtiie.mesgard.ui.conversation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomStateTest {
    private val state = ZoomState().apply { onLaidOut(Size(1000f, 2000f), 1000, 1000) }

    @Test
    fun `a photo starts at normal size`() {
        assertFalse(state.isZoomed)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `a double tap zooms in and a second one zooms out`() {
        state.toggleAt(Offset(500f, 1000f))
        assertTrue(state.isZoomed)

        state.toggleAt(Offset(500f, 1000f))
        assertFalse(state.isZoomed)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `a pinch cannot zoom past the limit or out below normal size`() {
        state.transform(Offset(500f, 1000f), Offset.Zero, 100f)
        assertEquals(5f, state.zoom, 0f)

        state.transform(Offset(500f, 1000f), Offset.Zero, 0.001f)
        assertEquals(1f, state.zoom, 0f)
    }

    @Test
    fun `a drag cannot take the photo past its own edges`() {
        state.transform(Offset(500f, 1000f), Offset.Zero, 2f)

        state.transform(Offset(500f, 1000f), Offset(5000f, 0f), 1f)

        assertEquals(500f, state.offset.x, 0f)
    }
}
