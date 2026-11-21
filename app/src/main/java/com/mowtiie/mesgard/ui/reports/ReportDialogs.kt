package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.ui.common.NoteField

private val FieldSpacing = 12.dp

/** Whichever question is open: closing a report, asking who reported, or the answer to that. */
@Composable
internal fun ReportDialogs(
    dialog: ReportDialog?,
    hasMessage: Boolean,
    onDecisionConfirmed: (String, Boolean) -> Unit,
    onRevealConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is ReportDialog.Decide -> DecisionDialog(dialog.status, hasMessage, onDecisionConfirmed, onDismiss)
        ReportDialog.Reveal -> RevealDialog(onRevealConfirmed, onDismiss)
        is ReportDialog.Revealed -> RevealedDialog(dialog.reporterName, onDismiss)
        null -> Unit
    }
}

/** Closing a report needs a note, so the decision can be understood later. A message can be removed with it. */
@Composable
private fun DecisionDialog(
    status: ReportStatus,
    hasMessage: Boolean,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var removeMessage by rememberSaveable { mutableStateOf(false) }
    val isResolve = status == ReportStatus.RESOLVED
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isResolve) R.string.report_decide_resolve_title else R.string.report_decide_dismiss_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
                NoteField(
                    value = note,
                    onValueChange = { note = it },
                    label = R.string.report_decide_note_label,
                    helper = stringResource(R.string.report_decide_note_helper),
                    maxLength = REPORT_NOTE_MAX_LENGTH,
                )
                if (hasMessage && isResolve) {
                    ListItem(
                        modifier = Modifier.toggleable(
                            value = removeMessage,
                            role = Role.Checkbox,
                            onValueChange = { removeMessage = it },
                        ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.report_decide_remove)) },
                        supportingContent = { Text(stringResource(R.string.report_decide_remove_summary)) },
                        trailingContent = { Checkbox(checked = removeMessage, onCheckedChange = null) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note, removeMessage && isResolve) },
                enabled = note.trim().length >= REPORT_DECISION_NOTE_MIN_LENGTH,
            ) {
                Text(stringResource(if (isResolve) R.string.report_action_resolve else R.string.report_action_dismiss))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Showing an anonymous reporter is the exception, so it needs a reason, and the reason is recorded. */
@Composable
private fun RevealDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_reveal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
                Text(stringResource(R.string.report_reveal_text))
                NoteField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = R.string.report_reveal_reason_label,
                    helper = stringResource(R.string.report_reveal_reason_helper, REVEAL_REASON_MIN_LENGTH),
                    maxLength = REPORT_NOTE_MAX_LENGTH,
                    modifier = Modifier.padding(top = FieldSpacing),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.trim().length >= REVEAL_REASON_MIN_LENGTH) {
                Text(stringResource(R.string.report_reveal_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RevealedDialog(reporterName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_revealed_title)) },
        text = { Text(reporterName) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.report_done)) } },
    )
}
