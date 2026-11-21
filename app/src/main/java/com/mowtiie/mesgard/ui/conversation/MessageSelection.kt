package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus

internal enum class PinChoice { NONE, PIN, UNPIN }

/** What the selection bar can ask for. Each one acts on the messages chosen, and the bar then leaves the selection. */
@Immutable
internal data class MessageSelectionCallbacks(
    val onToggle: (String) -> Unit = {},
    val onClear: () -> Unit = {},
    val onReply: (Message) -> Unit = {},
    val onDelete: (List<String>) -> Unit = {},
    val onEdit: (Message) -> Unit = {},
    val onInfo: (String) -> Unit = {},
    val onPin: (String) -> Unit = {},
    val onUnpin: (String) -> Unit = {},
    val onReport: (String) -> Unit = {},
)

/** The chosen messages, with what the person may do to pins, for the list and the selection bar. */
@Immutable
internal data class MessageSelection(
    val selectedIds: Set<String> = emptySet(),
    val canPin: Boolean = false,
    val pinnedIds: Set<String> = emptySet(),
    val callbacks: MessageSelectionCallbacks = MessageSelectionCallbacks(),
)

/** What the action bar offers for the messages that are selected. */
internal data class MessageSelectionActions(
    val canReply: Boolean,
    val canCopy: Boolean,
    val canDelete: Boolean,
    val canEdit: Boolean,
    val canShowInfo: Boolean,
    val canReport: Boolean,
    /** Whether the person may pin at all. A person who may not never sees the item, so it is not shown disabled. */
    val canManagePins: Boolean,
    val pin: PinChoice,
)

/**
 * Reply, edit, info and pin act on one message, so they are offered only for a single selection. Copy needs some text,
 * and delete is offered only when every selected message is the user's own and has been sent.
 */
internal fun selectionActionsFor(
    selected: List<Message>,
    canPin: Boolean,
    pinnedIds: Set<String>,
): MessageSelectionActions {
    val single = selected.singleOrNull()
    val isSentSingle = single != null && single.isSent()
    return MessageSelectionActions(
        canReply = isSentSingle,
        canCopy = selected.any { it.hasText() },
        canDelete = selected.isNotEmpty() && selected.all { it.isOwn && it.isSent() },
        canEdit = isSentSingle && single?.isOwn == true && single.body.isNotEmpty(),
        canShowInfo = isSentSingle && single?.isOwn == true,
        canReport = isSentSingle && single?.isOwn == false,
        canManagePins = canPin,
        pin = when {
            !isSentSingle || !canPin || single == null -> PinChoice.NONE
            single.id in pinnedIds -> PinChoice.UNPIN
            else -> PinChoice.PIN
        },
    )
}

/**
 * The messages that are selected. A selected message that is gone, for example deleted from another phone, takes
 * itself out of the selection.
 */
@Composable
internal fun rememberSelectedMessages(messages: List<Message>, selection: MessageSelection): List<Message> {
    val selected = remember(messages, selection.selectedIds) { messages.filter { it.id in selection.selectedIds } }
    LaunchedEffect(selected.size, selection.selectedIds.size) {
        if (selected.size != selection.selectedIds.size) selection.callbacks.onClear()
    }
    return selected
}

/** The selection bar for the top of the screen, with copy handled by the screen because it shows the confirmation. */
internal fun selectionBar(
    selected: List<Message>,
    selection: MessageSelection,
    onCopy: (String) -> Unit,
): @Composable () -> Unit = {
    MessageSelectionBar(
        selected = selected,
        actions = selectionActionsFor(selected, selection.canPin, selection.pinnedIds),
        callbacks = selection.callbacks,
        onCopy = onCopy,
    )
}

/** The text of the selected messages, the oldest first, one message per line. */
internal fun copyTextOf(selected: List<Message>): String = selected
    .filter { it.hasText() }
    .sortedBy { it.createdAtEpochMillis }
    .joinToString("\n") { it.body }

private fun Message.isSent() = status == MessageStatus.SENT && !isDeleted

private fun Message.hasText() = !isDeleted && body.isNotEmpty()
