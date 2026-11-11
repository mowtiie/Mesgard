package com.mowtiie.mesgard.data.attachment

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageScalingTest {
    @Test
    fun `keeps an image that already fits`() {
        assertEquals(1_600 to 1_200, scaledSize(1_600, 1_200, maxEdge = 2_048))
    }

    @Test
    fun `scales a landscape image to the maximum width and keeps its proportions`() {
        assertEquals(2_048 to 1_536, scaledSize(4_096, 3_072, maxEdge = 2_048))
    }

    @Test
    fun `scales a portrait image to the maximum height`() {
        assertEquals(1_024 to 2_048, scaledSize(3_000, 6_000, maxEdge = 2_048))
    }

    @Test
    fun `never scales a side down to zero`() {
        assertEquals(1, scaledSize(10_000, 1, maxEdge = 2_048).second)
    }

    @Test
    fun `decodes at full size when the image is not much larger than the limit`() {
        assertEquals(1, sampleSizeFor(3_000, 2_000, maxEdge = 2_048))
    }

    @Test
    fun `decodes at a reduced size while the result stays above the limit`() {
        assertEquals(4, sampleSizeFor(9_000, 6_000, maxEdge = 2_048))
    }
}
