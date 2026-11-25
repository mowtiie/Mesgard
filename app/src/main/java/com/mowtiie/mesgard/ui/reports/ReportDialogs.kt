package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private const val NO_BLOCK_DAYS = 0
private const val GROUP_NAME_MAX_LENGTH = 200

private val BlockOptions = listOf(
    NO_BLOCK_DAYS to RadioLabel(R.string.report_block_none),
    7 to RadioLabel(R.string.report_block_7),
    30 to RadioLabel(R.string.report_block_30),
)

/** Whichever question is open: closing a report, asking who reported, or the answer to that. */
@Composable
internal fun ReportDialogs(
    dialog: ReportDialog?,
    followUp: ReportFollowUp?,
    onDecisionConfirmed: (String, Boolean, Int?, String?) -> Unit,
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
    onConfirm: (String, Boolean, Int?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var applyFollowUp by rememberSaveable { mutableStateOf(false) }
    var blockDays by rememberSaveable { mutableStateOf(NO_BLOCK_DAYS) }
    var newGroupName by rememberSaveable { mutableStateOf("") }
    val renames = followUp == ReportFollowUp.RENAME_GROUP
    val isResolve = status == ReportStatus.RESOLVED
    FormSheet(
        title = stringResource(
            if (isResolve) R.string.report_decide_resolve_title else R.string.report_decide_dismiss_title,
        ),
        confirmLabel = stringResource(
            if (isResolve) R.string.report_action_resolve else R.string.report_action_dismiss,
        ),
        onConfirm = {
            onConfirm(
                note,
                applyFollowUp && isResolve,
                blockDays.takeIf { it != NO_BLOCK_DAYS },
                newGroupName.takeIf { renames && applyFollowUp },
            )
        },
        onDismiss = onDismiss,
        confirmEnabled = note.trim().length >= REPORT_DECISION_NOTE_MIN_LENGTH &&
            !(renames && applyFollowUp && newGroupName.isBlank()),
    ) {
        NoteField(
            value = note,
            onValueChange = { note = it },
            label = R.string.report_decide_note_label,
            helper = stringResource(R.string.report_decide_note_helper),
            maxLength = REPORT_NOTE_MAX_LENGTH,
        )
        if (followUp != null && isResolve) {
            FollowUpChoice(
                followUp = followUp,
                state = FollowUpState(applyFollowUp, blockDays, newGroupName),
                onApplyChanged = { applyFollowUp = it },
                onBlockDaysChanged = { blockDays = it },
                onNewGroupNameChanged = { newGroupName = it },
            )
        }
    }
}

private data class FollowUpState(val apply: Boolean, val blockDays: Int, val newGroupName: String)

/** The follow up a reviewer can apply with the decision, and what it needs: a block, or a new group name. */
@Composable
private fun FollowUpChoice(
    followUp: ReportFollowUp,
    state: FollowUpState,
    onApplyChanged: (Boolean) -> Unit,
    onBlockDaysChanged: (Int) -> Unit,
    onNewGroupNameChanged: (String) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = state.apply, role = Role.Checkbox, onValueChange = onApplyChanged),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(stringResource(followUp.titleResource())) },
        supportingContent = { Text(stringResource(followUp.summaryResource())) },
        trailingContent = { Checkbox(checked = state.apply, onCheckedChange = null) },
    )
    if (followUp == ReportFollowUp.RENAME_GROUP && state.apply) {
        OutlinedTextField(
            value = state.newGroupName,
            onValueChange = { onNewGroupNameChanged(it.take(GROUP_NAME_MAX_LENGTH)) },
            label = { Text(stringResource(R.string.report_rename_group_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (followUp == ReportFollowUp.REMOVE_PICTURE && state.apply) {
        RadioGroup(
            options = BlockOptions,
            selected = state.blockDays,
            onSelected = { onBlockDaysChanged(it ?: NO_BLOCK_DAYS) },
        )
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
