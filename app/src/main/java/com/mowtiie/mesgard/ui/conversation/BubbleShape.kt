package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LargeBubbleCorner = 16.dp
internal val SmallBubbleCorner = 4.dp

/** The corner radii of a bubble, from the side of the screen it is on: the start side of a received message. */
internal data class BubbleCorners(val topStart: Dp, val topEnd: Dp, val bottomEnd: Dp, val bottomStart: Dp)

/**
 * The corners next to the sender are small, so a run of bubbles from one person reads as one stack and the last has
 * a tail. Only the first bubble of a run has a round top on that side.
 */
internal fun bubbleCornersFor(isOwn: Boolean, startsGroup: Boolean): BubbleCorners {
    val joinedTop = if (startsGroup) LargeBubbleCorner else SmallBubbleCorner
    return if (isOwn) {
        BubbleCorners(LargeBubbleCorner, joinedTop, SmallBubbleCorner, LargeBubbleCorner)
    } else {
        BubbleCorners(joinedTop, LargeBubbleCorner, LargeBubbleCorner, SmallBubbleCorner)
    }
}

internal fun bubbleShape(isOwn: Boolean, startsGroup: Boolean): Shape = bubbleCornersFor(isOwn, startsGroup).let {
    RoundedCornerShape(
        topStart = it.topStart,
        topEnd = it.topEnd,
        bottomEnd = it.bottomEnd,
        bottomStart = it.bottomStart,
    )
}
