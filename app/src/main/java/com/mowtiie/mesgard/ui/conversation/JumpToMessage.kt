package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal const val MAX_JUMP_LOADS = 40

/** What to do next on the way to a message the person picked in the search results. */
internal sealed interface JumpStep {
    /** The message is in the list at [index]. */
    data class Scroll(val index: Int) : JumpStep

    /** Older messages have to be loaded first. */
    data object LoadOlder : JumpStep

    /** An earlier load is still running. */
    data object Wait : JumpStep

    /** The message cannot be reached, because the history ends or too many pages were loaded. */
    data object GiveUp : JumpStep
}

/**
 * Decides one step of the jump. The list holds only the newest messages of the conversation, so a hit from long ago
 * is reached by loading older pages until it appears, never by skipping over the history in between.
 */
internal fun jumpStep(
    timeline: List<TimelineItem>,
    messageId: String,
    isLoadingOlder: Boolean,
    isHistoryComplete: Boolean,
    loadsSoFar: Int,
): JumpStep {
    val index = timeline.indexOfFirst { it is TimelineItem.Bubble && it.message.id == messageId }
    return when {
        index >= 0 -> JumpStep.Scroll(index)
        isLoadingOlder -> JumpStep.Wait
        isHistoryComplete || loadsSoFar >= MAX_JUMP_LOADS -> JumpStep.GiveUp
        else -> JumpStep.LoadOlder
    }
}

/** Takes the list to the message the person chose in the search, loading older pages when it is not there yet. */
@Composable
internal fun JumpToMessageEffect(
    jumpTo: String?,
    timeline: List<TimelineItem>,
    isLoadingOlder: Boolean,
    isHistoryComplete: Boolean,
    listState: LazyListState,
    onJumpHandled: () -> Unit,
    onJumpFailed: () -> Unit,
    onLoadOlder: () -> Unit,
) {
    var loads by remember { mutableIntStateOf(0) }
    LaunchedEffect(jumpTo, timeline, isLoadingOlder) {
        if (jumpTo == null) {
            loads = 0
            return@LaunchedEffect
        }
        when (val step = jumpStep(timeline, jumpTo, isLoadingOlder, isHistoryComplete, loads)) {
            is JumpStep.Scroll -> {
                listState.animateScrollToItem(step.index)
                onJumpHandled()
            }
            JumpStep.LoadOlder -> {
                loads += 1
                onLoadOlder()
            }
            JumpStep.Wait -> Unit
            JumpStep.GiveUp -> onJumpFailed()
        }
    }
}
