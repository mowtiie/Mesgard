package com.mowtiie.mesgard.ui.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.ui.common.ExternalLinkIcon
import com.mowtiie.mesgard.ui.common.SectionHeader

private val ContentPadding = 16.dp
private val BlockPadding = 8.dp

private class SummaryBlock(@StringRes val title: Int, @StringRes val text: Int)

private val SummaryBlocks = listOf(
    SummaryBlock(R.string.privacy_block_stored, R.string.privacy_summary_stored),
    SummaryBlock(R.string.privacy_block_readers, R.string.privacy_summary_not_encrypted),
    SummaryBlock(R.string.privacy_block_edits, R.string.privacy_summary_edits),
    SummaryBlock(R.string.privacy_block_groups, R.string.privacy_summary_groups),
    SummaryBlock(R.string.privacy_block_retention, R.string.privacy_summary_retention),
    SummaryBlock(R.string.privacy_block_online, R.string.privacy_summary_online),
    SummaryBlock(R.string.privacy_block_reports, R.string.privacy_summary_reports),
    SummaryBlock(R.string.privacy_block_device, R.string.privacy_summary_device),
    SummaryBlock(R.string.privacy_block_questions, R.string.privacy_summary_questions),
)

/** Plain language about how the app handles data, true for every organization, for adults and for children alike. */
@Composable
internal fun DataSummary() {
    SectionHeader(R.string.privacy_summary_title)
    SummaryBlocks.forEach { block ->
        Column(modifier = Modifier.padding(horizontal = ContentPadding, vertical = BlockPadding)) {
            Text(text = stringResource(block.title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(block.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The links of one organization, or a note that it has not added a notice yet. */
@Composable
internal fun NoticeLinks(notice: PrivacyNotice) {
    if (!notice.hasLinks) {
        Text(
            text = stringResource(R.string.privacy_notice_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = BlockPadding),
        )
    }
    notice.noticeUrl?.let { NoticeLinkRow(R.string.privacy_notice_read, it) }
    notice.childNoticeUrl?.let { NoticeLinkRow(R.string.privacy_notice_read_children, it) }
}

@Composable
private fun NoticeLinkRow(@StringRes title: Int, url: String) {
    val uriHandler = LocalUriHandler.current
    ListItem(
        modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(url) },
        leadingContent = { Icon(painterResource(R.drawable.ic_description), contentDescription = null) },
        headlineContent = { Text(stringResource(title)) },
        trailingContent = { ExternalLinkIcon() },
    )
}
