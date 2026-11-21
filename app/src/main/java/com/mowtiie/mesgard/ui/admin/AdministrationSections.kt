package com.mowtiie.mesgard.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.audit.AuditEntryItem
import com.mowtiie.mesgard.ui.admin.audit.rememberAuditLabels
import com.mowtiie.mesgard.ui.common.SectionHeader

private val ContentPadding = 16.dp
private val ButtonSpacing = 8.dp

/** The two things an admin does most: add one person, or import a whole roster. */
@Composable
internal fun QuickActions(navigation: AdminNavigation) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ContentPadding),
        horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
    ) {
        FilledTonalButton(onClick = navigation.onAddMember, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.members_add), modifier = Modifier.padding(start = ButtonSpacing))
        }
        FilledTonalButton(onClick = navigation.onImportMembers, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_upload), contentDescription = null)
            Text(stringResource(R.string.members_import), modifier = Modifier.padding(start = ButtonSpacing))
        }
    }
}

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
