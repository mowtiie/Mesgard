package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal const val JUMP_HIGHLIGHT_MILLIS = 1_500L

/** The message the list just scrolled to, so the person can find it among the others. It clears itself. */
@Stable
internal class JumpHighlight {
    var messageId: String? by mutableStateOf(null)
        private set

    fun show(id: String) {
        messageId = id
    }

    fun clear() {
        messageId = null
    }
}

@Composable
internal fun rememberJumpHighlight(): JumpHighlight {
    val highlight = remember { JumpHighlight() }
    LaunchedEffect(highlight.messageId) {
        if (highlight.messageId == null) return@LaunchedEffect
        delay(JUMP_HIGHLIGHT_MILLIS)
        highlight.clear()
    }
    return highlight
}
