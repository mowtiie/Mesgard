package com.mowtiie.mesgard.ui.chats

/** What the selection bar can do, so the bar and the screen share one list of callbacks. */
internal data class SelectionCallbacks(
    val onToggled: (String) -> Unit,
    val onSelectAll: () -> Unit,
    val onClose: () -> Unit,
    val onErrorShown: () -> Unit,
    val onPin: () -> Unit,
    val onArchive: () -> Unit,
    val onUndoArchiveHandled: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onDeleteDismissed: () -> Unit,
    val onUndoDeleteHandled: (Boolean) -> Unit,
    val onMute: () -> Unit,
    val onRead: () -> Unit,
)
