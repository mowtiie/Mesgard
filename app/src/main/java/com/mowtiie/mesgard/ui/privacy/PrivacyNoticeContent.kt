package com.mowtiie.mesgard.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.PrivacyNotice

private val ContentPadding = 16.dp
private val ParagraphSpacing = 12.dp

private val SummaryParagraphs = listOf(
    R.string.privacy_summary_stored,
    R.string.privacy_summary_not_encrypted,
    R.string.privacy_summary_edits,
    R.string.privacy_summary_retention,
    R.string.privacy_summary_online,
    R.string.privacy_summary_reports,
    R.string.privacy_summary_device,
    R.string.privacy_summary_questions,
)

/** Plain language about how the app handles data, true for every organization, for adults and for children alike. */
@Composable
internal fun DataSummary() {
    Column(
        modifier = Modifier.padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(ParagraphSpacing),
    ) {
        Text(
            text = stringResource(R.string.privacy_summary_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        SummaryParagraphs.forEach { paragraph ->
            Text(text = stringResource(paragraph), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The links of one organization, or a note that it has not added a notice yet. */
@Composable
internal fun NoticeLinks(notice: PrivacyNotice, showOrganization: Boolean) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(ParagraphSpacing),
    ) {
        if (showOrganization) {
            Text(
                text = notice.orgName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (!notice.hasLinks) {
            Text(
                text = stringResource(R.string.privacy_notice_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notice.noticeUrl?.let { url ->
            OutlinedButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_notice_read))
            }
        }
        notice.childNoticeUrl?.let { url ->
            OutlinedButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_notice_read_children))
            }
        }
    }
}
