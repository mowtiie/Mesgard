package com.mowtiie.mesgard.ui.chats

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** What happens after a selection action: an error message, Undo for archive and delete, and the delete dialog. */
@Composable
internal fun SelectionEffects(
    selection: ConversationSelectionUiState,
    callbacks: SelectionCallbacks,
    snackbarHostState: SnackbarHostState,
) {
    val errorMessage = selection.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            callbacks.onErrorShown()
        }
    }
    val undoLabel = stringResource(R.string.undo)
    val archived = selection.undoArchive.size
    val archivedMessage = pluralStringResource(R.plurals.archived_snackbar, archived, archived)
    LaunchedEffect(selection.undoArchive) {
        if (selection.undoArchive.isEmpty()) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(archivedMessage, undoLabel, duration = SnackbarDuration.Long)
        callbacks.onUndoArchiveHandled(result == SnackbarResult.ActionPerformed)
    }
    val deleted = selection.deletion.undo.size
    val deletedMessage = pluralStringResource(R.plurals.deleted_snackbar, deleted, deleted)
    LaunchedEffect(selection.deletion.undo) {
        if (selection.deletion.undo.isEmpty()) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
        callbacks.onUndoDeleteHandled(result == SnackbarResult.ActionPerformed)
    }
    if (selection.deletion.pending.isNotEmpty()) {
        DeleteConversationsDialog(
            selection.deletion.pending.size,
            callbacks.onDeleteConfirmed,
            callbacks.onDeleteDismissed,
        )
    }
}

/** Deleting clears the history for this person only, and a new message brings the conversation back. */
@Composable
private fun DeleteConversationsDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.conversation_delete_title, count, count)) },
        text = { Text(stringResource(R.string.conversation_delete_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.conversation_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
