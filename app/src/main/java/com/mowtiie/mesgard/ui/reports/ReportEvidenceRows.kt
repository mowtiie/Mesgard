package com.mowtiie.mesgard.ui.reports

import android.text.format.Formatter
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.EvidenceFile
import com.mowtiie.mesgard.domain.model.EvidenceLine
import com.mowtiie.mesgard.domain.model.ReportEvent

@Composable
internal fun EvidenceRow(line: EvidenceLine) {
    val formatTime = rememberDateTimeFormatter()
    ListItem(
        overlineContent = { Text(formatTime(line.sentAtEpochMillis)) },
        headlineContent = { Text(line.senderName) },
        supportingContent = { Text(line.body.ifEmpty { stringResource(R.string.report_message_no_text) }) },
    )
}

@Composable
internal fun FileRow(file: EvidenceFile) {
    val size = Formatter.formatShortFileSize(LocalContext.current, file.sizeBytes)
    ListItem(
        headlineContent = { Text(file.fileName) },
        supportingContent = { Text(stringResource(R.string.report_row_summary, file.mimeType, size)) },
    )
}

@Composable
internal fun EventRow(event: ReportEvent) {
    val formatTime = rememberDateTimeFormatter()
    val actor = event.actorName ?: stringResource(R.string.report_reporter_anonymous)
    ListItem(
        headlineContent = { Text(stringResource(event.type.labelResource())) },
        supportingContent = {
            val line = stringResource(R.string.report_row_summary, actor, formatTime(event.createdAtEpochMillis))
            Text(listOfNotNull(line, event.note).joinToString("\n"))
        },
    )
}
