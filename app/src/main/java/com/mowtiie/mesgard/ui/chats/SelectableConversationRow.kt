package com.mowtiie.mesgard.ui.chats

import androidx.compose.runtime.Composable
import com.mowtiie.mesgard.domain.model.ConversationSummary

/** A tap opens the conversation, or adds it to the selection once a selection has started. */
@Composable
internal fun SelectableConversationRow(
    conversation: ConversationSummary,
    nowEpochMillis: Long,
    selection: ConversationSelectionUiState,
    callbacks: SelectionCallbacks,
    onOpen: (String) -> Unit,
) {
    ConversationRow(
        conversation = conversation,
        nowEpochMillis = nowEpochMillis,
        onClick = { if (selection.isActive) callbacks.onToggled(conversation.id) else onOpen(conversation.id) },
        isSelected = conversation.id in selection.selectedIds,
        onLongClick = { callbacks.onToggled(conversation.id) },
    )
}
