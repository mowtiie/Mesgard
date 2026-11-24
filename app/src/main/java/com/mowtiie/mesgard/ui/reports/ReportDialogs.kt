package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.common.NoteField

/** Whichever question is open: closing a report, asking who reported, or the answer to that. */
@Composable
internal fun ReportDialogs(
    dialog: ReportDialog?,
    followUp: ReportFollowUp?,
    onDecisionConfirmed: (String, Boolean) -> Unit,
    onRevealConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is ReportDialog.Decide -> DecisionSheet(dialog.status, followUp, onDecisionConfirmed, onDismiss)
        ReportDialog.Reveal -> RevealSheet(onRevealConfirmed, onDismiss)
        is ReportDialog.Revealed -> RevealedDialog(dialog.reporterName, onDismiss)
        null -> Unit
    }
}

/** Closing a report needs a note, so the decision can be understood later. A follow up action can come with it. */
@Composable
private fun DecisionSheet(
    status: ReportStatus,
    followUp: ReportFollowUp?,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var applyFollowUp by rememberSaveable { mutableStateOf(false) }
    val isResolve = status == ReportStatus.RESOLVED
    FormSheet(
        title = stringResource(
            if (isResolve) R.string.report_decide_resolve_title else R.string.report_decide_dismiss_title,
        ),
        confirmLabel = stringResource(
            if (isResolve) R.string.report_action_resolve else R.string.report_action_dismiss,
        ),
        onConfirm = { onConfirm(note, applyFollowUp && isResolve) },
        onDismiss = onDismiss,
        confirmEnabled = note.trim().length >= REPORT_DECISION_NOTE_MIN_LENGTH,
    ) {
        NoteField(
            value = note,
            onValueChange = { note = it },
            label = R.string.report_decide_note_label,
            helper = stringResource(R.string.report_decide_note_helper),
            maxLength = REPORT_NOTE_MAX_LENGTH,
        )
        if (followUp != null && isResolve) {
            ListItem(
                modifier = Modifier.toggleable(
                    value = applyFollowUp,
                    role = Role.Checkbox,
                    onValueChange = { applyFollowUp = it },
                ),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(stringResource(followUp.titleResource())) },
                supportingContent = { Text(stringResource(followUp.summaryResource())) },
                trailingContent = { Checkbox(checked = applyFollowUp, onCheckedChange = null) },
            )
        }
    }
}

/** Showing an anonymous reporter is the exception, so it needs a reason, and the reason is recorded. */
@Composable
private fun RevealSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = stringResource(R.string.report_reveal_title),
        confirmLabel = stringResource(R.string.report_reveal_confirm),
        onConfirm = { onConfirm(reason) },
        onDismiss = onDismiss,
        confirmEnabled = reason.trim().length >= REVEAL_REASON_MIN_LENGTH,
    ) {
        Text(
            text = stringResource(R.string.report_reveal_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NoteField(
            value = reason,
            onValueChange = { reason = it },
            label = R.string.report_reveal_reason_label,
            helper = stringResource(R.string.report_reveal_reason_helper, REVEAL_REASON_MIN_LENGTH),
            maxLength = REPORT_NOTE_MAX_LENGTH,
        )
    }
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
