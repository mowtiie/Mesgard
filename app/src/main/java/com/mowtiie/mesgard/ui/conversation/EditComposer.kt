package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val EditPadding = 8.dp
private const val EDIT_MAX_LINES = 5

/** What the edit bar can do. */
internal data class EditActions(
    val onTextChanged: (String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
)

/** Replaces the composer while a sent message is changed, with the way out and the way to save in plain sight. */
@Composable
internal fun EditComposer(state: MessageEditUiState, actions: EditActions, modifier: Modifier = Modifier) {
    val messageLabel = stringResource(R.string.message_label)
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(EditPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.message_editing),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).padding(start = EditPadding),
                )
                IconButton(onClick = actions.onCancel, enabled = !state.isSaving) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.message_edit_cancel),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(EditPadding)) {
                MessageTextField(
                    value = state.text,
                    onValueChange = actions.onTextChanged,
                    maxLines = EDIT_MAX_LINES,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).semantics { contentDescription = messageLabel },
                )
                FilledIconButton(
                    onClick = actions.onSave,
                    enabled = state.canSave,
                    modifier = Modifier.size(MessageFieldHeight),
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.message_edit_save),
                    )
                }
            }
        }
    }
}
