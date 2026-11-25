package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.EvidenceLine
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.ui.common.SectionHeader

private val ContentPadding = 16.dp
private val ButtonSpacing = 8.dp

internal data class ReportActions(
    val enabled: Boolean,
    val onStartReview: () -> Unit,
    val onDecide: (ReportStatus) -> Unit,
)

internal fun LazyListScope.reportDetailItems(detail: ReportDetail, actions: ReportActions) {
    if (detail.category == ReportCategory.CHILD_SAFETY) item(key = "child-safety") { ChildSafetyNotice() }
    item(key = "summary") { Summary(detail) }
    if (!detail.status.isClosed) item(key = "actions") { ActionButtons(detail.status, actions) }
    val isTextReport = detail.subject in setOf(ReportSubject.NAME, ReportSubject.USERNAME, ReportSubject.GROUP_NAME)
    val isPictureReport = detail.subject in setOf(ReportSubject.PROFILE_PICTURE, ReportSubject.GROUP_PICTURE)
    item(key = "message-heading") { SectionHeader(detail.subject.sectionResource()) }
    val evidence = detail.evidence
    if (evidence == null) {
        if (isPictureReport) {
            item(key = "picture") { EvidencePicture(detail.files.firstOrNull()?.storagePath) }
        } else if (isTextReport && detail.subjectText != null) {
            item(key = "name") { EvidenceName(detail.subjectText, detail.subject) }
        } else {
            item(key = "person-only") { Note(R.string.report_person_only) }
        }
    } else {
        item(key = "message") { EvidenceMessage(evidence.conversationTitle, evidence.message, detail.isMessageRemoved) }
        if (evidence.earlier.isNotEmpty()) {
            item(key = "earlier-heading") { SectionHeader(R.string.report_section_earlier) }
            items(evidence.earlier, key = { "earlier-${it.sentAtEpochMillis}-${it.senderName}" }) { EvidenceRow(it) }
        }
    }
    if (detail.files.isNotEmpty()) {
        item(key = "files-heading") { SectionHeader(R.string.report_section_files) }
        items(detail.files, key = { "file-${it.fileName}-${it.sizeBytes}" }) { FileRow(it) }
    }
    item(key = "history-heading") { SectionHeader(R.string.report_section_history) }
    items(detail.events, key = { "event-${it.type}-${it.createdAtEpochMillis}-${it.actorName}" }) { EventRow(it) }
}

@Composable
private fun ChildSafetyNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.padding(ContentPadding),
    ) {
        Text(
            text = stringResource(R.string.report_child_safety_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = ContentPadding, top = ContentPadding, end = ContentPadding),
        )
        Text(
            text = stringResource(R.string.report_child_safety_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(ContentPadding),
        )
    }
}

@Composable
private fun Summary(detail: ReportDetail) {
    val formatTime = rememberDateTimeFormatter()
    val reporter = detail.reporterName ?: stringResource(R.string.report_reporter_anonymous)
    Field(R.string.report_field_status, stringResource(detail.status.labelResource()))
    Field(R.string.report_field_subject, stringResource(detail.subject.labelResource()))
    Field(R.string.report_field_type, stringResource(detail.category.labelResource()))
    val isGroupReport = detail.subject == ReportSubject.GROUP_PICTURE || detail.subject == ReportSubject.GROUP_NAME
    Field(if (isGroupReport) R.string.report_field_group else R.string.report_field_person, detail.subjectName)
    Field(R.string.report_field_reporter, reporter)
    Field(R.string.report_field_date, formatTime(detail.createdAtEpochMillis))
    detail.note?.let { Field(R.string.report_field_details, it) }
    detail.resolutionNote?.let { Field(R.string.report_field_decision, it) }
}

@Composable
private fun Field(label: Int, value: String) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = { Text(value) })
}

@Composable
private fun ActionButtons(status: ReportStatus, actions: ReportActions) {
    Row(
        modifier = Modifier.padding(horizontal = ContentPadding),
        horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
    ) {
        if (status == ReportStatus.OPEN) {
            OutlinedButton(onClick = actions.onStartReview, enabled = actions.enabled) {
                Text(stringResource(R.string.report_action_start))
            }
        }
        Button(onClick = { actions.onDecide(ReportStatus.RESOLVED) }, enabled = actions.enabled) {
            Text(stringResource(R.string.report_action_resolve))
        }
        OutlinedButton(onClick = { actions.onDecide(ReportStatus.DISMISSED) }, enabled = actions.enabled) {
            Text(stringResource(R.string.report_action_dismiss))
        }
    }
}

@Composable
private fun EvidenceName(name: String, subject: ReportSubject) {
    val note = if (subject ==
        ReportSubject.USERNAME
    ) {
        R.string.report_username_copy_note
    } else {
        R.string.report_name_copy_note
    }
    ListItem(
        overlineContent = { Text(stringResource(note)) },
        headlineContent = { Text(name) },
    )
}

@Composable
private fun Note(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(ContentPadding),
    )
}

@Composable
private fun EvidenceMessage(conversationTitle: String?, message: EvidenceLine, isRemoved: Boolean) {
    val formatTime = rememberDateTimeFormatter()
    val place = conversationTitle ?: stringResource(R.string.report_direct_message)
    ListItem(
        overlineContent = {
            Text(stringResource(R.string.report_row_summary, place, formatTime(message.sentAtEpochMillis)))
        },
        headlineContent = { Text(message.senderName) },
        supportingContent = { Text(message.body.ifEmpty { stringResource(R.string.report_message_no_text) }) },
    )
    Note(if (isRemoved) R.string.report_message_removed else R.string.report_message_copy_note)
}
