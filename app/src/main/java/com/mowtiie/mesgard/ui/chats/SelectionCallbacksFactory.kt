package com.mowtiie.mesgard.ui.chats

/** Connects the selection bar to the ViewModel; [visibleIds] is what Select all takes. */
internal fun ConversationSelectionViewModel.callbacks(
    selection: ConversationSelectionUiState,
    visibleIds: () -> List<String>,
) = SelectionCallbacks(
    onToggled = ::onToggled,
    onSelectAll = { onSelectAll(visibleIds()) },
    onClose = ::onSelectionClosed,
    onErrorShown = ::onErrorShown,
    onPin = ::onPinClicked,
    onArchive = ::onArchiveClicked,
    onUndoArchiveHandled = ::onUndoArchiveHandled,
    onDelete = { deletion.onRequested(selection.selectedIds) },
    onDeleteConfirmed = {
        deletion.onConfirmed()
        onSelectionClosed()
    },
    onDeleteDismissed = deletion::onDismissed,
    onUndoDeleteHandled = deletion::onUndoHandled,
    onMute = ::onMuteClicked,
    onRead = ::onReadClicked,
)
