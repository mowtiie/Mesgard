package com.mowtiie.mesgard.ui.reports

import android.text.format.Formatter
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.data.attachment.StoredFile
import com.mowtiie.mesgard.domain.model.EvidenceFile
import com.mowtiie.mesgard.domain.model.EvidenceLine
import com.mowtiie.mesgard.domain.model.ReportEvent
import com.mowtiie.mesgard.ui.common.LocalAvatarSource

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

private val EvidencePictureSize = 240.dp

/** The profile picture a report keeps, loaded with the reviewer's own session. It stays on the phone encrypted. */
@Composable
internal fun EvidencePicture(storagePath: String?) {
    val source = LocalAvatarSource.current
    val path by produceState<String?>(initialValue = null, source, storagePath) {
        value = if (source != null && storagePath != null) source.evidencePath(storagePath) else null
    }
    val shown = path
    if (shown == null) {
        Text(
            text = stringResource(R.string.report_picture_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    } else {
        AsyncImage(
            model = StoredFile(shown),
            contentDescription = stringResource(R.string.report_picture_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.padding(16.dp).size(EvidencePictureSize).clip(RoundedCornerShape(12.dp)),
        )
    }
}
