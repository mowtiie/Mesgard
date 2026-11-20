package com.mowtiie.mesgard.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleShapeTest {
    @Test
    fun `a received message has its tail on the start side`() {
        val corners = bubbleCornersFor(isOwn = false, startsGroup = true)

        assertEquals(SmallBubbleCorner, corners.bottomStart)
        assertEquals(LargeBubbleCorner, corners.bottomEnd)
        assertEquals(LargeBubbleCorner, corners.topStart)
    }

    @Test
    fun `an own message has its tail on the end side`() {
        val corners = bubbleCornersFor(isOwn = true, startsGroup = true)

        assertEquals(SmallBubbleCorner, corners.bottomEnd)
        assertEquals(LargeBubbleCorner, corners.bottomStart)
        assertEquals(LargeBubbleCorner, corners.topEnd)
    }

    @Test
    fun `a message that continues a run is joined to the one above on the sender side`() {
        assertEquals(SmallBubbleCorner, bubbleCornersFor(isOwn = false, startsGroup = false).topStart)
        assertEquals(SmallBubbleCorner, bubbleCornersFor(isOwn = true, startsGroup = false).topEnd)
    }

    @Test
    fun `the other side stays round in a run`() {
        assertEquals(LargeBubbleCorner, bubbleCornersFor(isOwn = false, startsGroup = false).topEnd)
        assertEquals(LargeBubbleCorner, bubbleCornersFor(isOwn = true, startsGroup = false).topStart)
    }
}
