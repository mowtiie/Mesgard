package com.mowtiie.mesgard.ui.conversation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val DOT_COUNT = 3
private const val DOT_CYCLE_MS = 1_200
private const val DOT_STAGGER_MS = 160
private const val DOT_PEAK_MS = DOT_CYCLE_MS / 4
private const val DOT_FALL_MS = DOT_CYCLE_MS / 2
private const val DOT_MIN_ALPHA = 0.3f
private val DotSize = 8.dp
private val BubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)

/** The incoming bubble with three pulsing dots that shows while someone else types. It reads out who is typing. */
@Composable
internal fun TypingBubble(names: List<String>, modifier: Modifier = Modifier) {
    val description = typingText(names)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, BubbleShape)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            repeat(DOT_COUNT) { index ->
                val alpha by transition.animateFloat(
                    initialValue = DOT_MIN_ALPHA,
                    targetValue = DOT_MIN_ALPHA,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = DOT_CYCLE_MS
                            DOT_MIN_ALPHA at 0 using LinearEasing
                            1f at DOT_PEAK_MS using LinearEasing
                            DOT_MIN_ALPHA at DOT_FALL_MS using LinearEasing
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(index * DOT_STAGGER_MS),
                    ),
                    label = "dot",
                )
                Box(
                    Modifier
                        .size(DotSize)
                        .alpha(alpha)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                )
            }
        }
    }
}

private const val TYPING_BUBBLE_KEY = "typing-bubble"

/** The bubble is the newest item of the reversed list, so it sits at the bottom. */
internal fun LazyListScope.typingBubbleItem(names: List<String>) {
    if (names.isNotEmpty()) item(key = TYPING_BUBBLE_KEY) { TypingBubble(names) }
}

/** Someone reading older messages is not pulled down; someone at the bottom sees the bubble appear. */
@Composable
internal fun ShowTypingBubbleEffect(isShown: Boolean, listState: LazyListState) {
    LaunchedEffect(isShown) {
        if (isShown && listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }
}
