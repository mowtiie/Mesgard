package com.mowtiie.mesgard.ui.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.audit.AuditEntryItem
import com.mowtiie.mesgard.ui.admin.audit.rememberAuditLabels
import com.mowtiie.mesgard.ui.common.SectionHeader

private val ContentPadding = 16.dp
private val ButtonSpacing = 8.dp

/** The latest changes, so a glance shows what happened without opening the whole log. */
@Composable
internal fun RecentActivitySection(uiState: AdministrationUiState) {
    SectionHeader(R.string.admin_section_recent_activity)
    if (uiState.showNoActivity) {
        Text(
            text = stringResource(R.string.audit_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = ButtonSpacing),
        )
        return
    }
    val labels = rememberAuditLabels()
    uiState.recentActivity.forEach { entry -> AuditEntryItem(entry, uiState.names, labels) }
}
