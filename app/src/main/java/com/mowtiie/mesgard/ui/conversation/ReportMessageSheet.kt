package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel
import com.mowtiie.mesgard.ui.settings.SwitchRow

private val SheetPadding = 24.dp
private val SheetSpacing = 16.dp
private val ProgressSize = 18.dp
private val ProgressStroke = 2.dp
private const val NOTE_MIN_LINES = 2
private const val NOTE_MAX_LINES = 4

private val CategoryLabels = listOf(
    ReportCategory.BULLYING to RadioLabel(R.string.report_category_bullying),
    ReportCategory.HARASSMENT to RadioLabel(R.string.report_category_harassment),
    ReportCategory.CHILD_SAFETY to RadioLabel(
        R.string.report_category_child_safety,
        R.string.report_category_child_safety_summary,
    ),
    ReportCategory.OTHER to RadioLabel(R.string.report_category_other),
)

internal data class ReportMessageActions(
    val onCategoryChanged: (ReportCategory) -> Unit,
    val onNoteChanged: (String) -> Unit,
    val onAnonymousChanged: (Boolean) -> Unit,
    val onSend: () -> Unit,
    val onDismiss: () -> Unit,
)

/** Asks what is wrong with a message, then says the report was sent. It stays on screen after a failure. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportMessageSheet(state: ReportMessageUiState, actions: ReportMessageActions) {
    // The rows are list items on the surface color, so the sheet takes it too, and it opens fully, as it is a form.
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = SheetPadding),
            verticalArrangement = Arrangement.spacedBy(SheetSpacing),
        ) {
            if (state.stage == ReportStage.SENT) {
                ReportSent(state.error == ReportError.ALREADY_REPORTED, actions.onDismiss)
            } else {
                ReportForm(state, actions)
            }
        }
    }
}

@Composable
private fun ReportForm(state: ReportMessageUiState, actions: ReportMessageActions) {
    Text(
        text = stringResource(R.string.report_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = SheetPadding).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.report_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SheetPadding),
    )
    RadioGroup(
        options = CategoryLabels,
        selected = state.category,
        onSelected = { chosen -> chosen?.let(actions.onCategoryChanged) },
        enabled = state.stage == ReportStage.FORM,
    )
    OutlinedTextField(
        value = state.note,
        onValueChange = actions.onNoteChanged,
        label = { Text(stringResource(R.string.report_note_label)) },
        minLines = NOTE_MIN_LINES,
        maxLines = NOTE_MAX_LINES,
        enabled = state.stage == ReportStage.FORM,
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetPadding),
    )
    SwitchRow(
        title = R.string.report_anonymous_title,
        summary = R.string.report_anonymous_summary,
        checked = state.isAnonymous,
        onCheckedChange = actions.onAnonymousChanged,
    )
    state.error?.let {
        Text(
            text = stringResource(it.messageResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = SheetPadding),
        )
    }
    SendButton(state, actions.onSend)
}

@Composable
private fun SendButton(state: ReportMessageUiState, onSend: () -> Unit) {
    val sending = stringResource(R.string.report_sending)
    Button(
        onClick = onSend,
        enabled = state.canSend || state.stage == ReportStage.SENDING,
        modifier = Modifier.padding(horizontal = SheetPadding).fillMaxWidth(),
    ) {
        if (state.stage == ReportStage.SENDING) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize).semantics { contentDescription = sending },
                strokeWidth = ProgressStroke,
                color = LocalContentColor.current,
            )
        } else {
            Text(stringResource(R.string.report_send))
        }
    }
}

@Composable
private fun ReportSent(alreadyReported: Boolean, onDone: () -> Unit) {
    Text(
        text = stringResource(R.string.report_sent_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = SheetPadding).semantics { heading() },
    )
    Text(
        text = stringResource(if (alreadyReported) R.string.report_already_sent_text else R.string.report_sent_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SheetPadding),
    )
    Button(onClick = onDone, modifier = Modifier.padding(horizontal = SheetPadding).fillMaxWidth()) {
        Text(stringResource(R.string.report_done))
    }
}

private fun ReportError.messageResource(): Int = when (this) {
    ReportError.NETWORK_UNAVAILABLE -> R.string.report_error_network
    ReportError.NOT_ALLOWED -> R.string.report_error_not_allowed
    ReportError.ALREADY_REPORTED, ReportError.UNKNOWN -> R.string.report_error_unknown
}
