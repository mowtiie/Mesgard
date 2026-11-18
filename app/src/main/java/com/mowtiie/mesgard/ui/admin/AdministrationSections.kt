package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.audit.AuditEntryItem
import com.mowtiie.mesgard.ui.admin.audit.rememberAuditLabels

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
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.members_add), modifier = Modifier.padding(start = ButtonSpacing))
        }
        FilledTonalButton(onClick = navigation.onImportMembers, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.members_import))
        }
    }
}

@Composable
internal fun ManageSection(uiState: AdministrationUiState, navigation: AdminNavigation) {
    SectionHeading(R.string.admin_section_manage)
    ManageRow(
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        title = R.string.members_title,
        summary = membersSummary(uiState),
        onClick = navigation.onOpenMembers,
    )
    ManageRow(
        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
        title = R.string.former_members_title,
        summary = stringResource(R.string.settings_former_members_summary),
        onClick = navigation.onOpenFormerMembers,
    )
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_group_outline), contentDescription = null) },
        title = R.string.groups_title,
        summary = uiState.groupCount?.let { pluralStringResource(R.plurals.admin_groups_count, it, it) }
            ?: stringResource(R.string.settings_groups_summary),
        onClick = navigation.onOpenGroups,
    )
    ManageRow(
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
        title = R.string.audit_title,
        summary = stringResource(R.string.settings_audit_summary),
        onClick = navigation.onOpenAuditLog,
    )
}

@Composable
private fun membersSummary(uiState: AdministrationUiState): String {
    val count = uiState.memberCount
    return when {
        count == null || count == 0 -> stringResource(R.string.settings_members_summary)
        uiState.isMemberCountAtLeast -> stringResource(R.string.admin_members_count_at_least, count)
        else -> pluralStringResource(R.plurals.admin_members_count, count, count)
    }
}

/** The latest changes, so a glance shows what happened without opening the whole log. */
@Composable
internal fun RecentActivitySection(uiState: AdministrationUiState, onOpenAuditLog: () -> Unit) {
    SectionHeading(R.string.admin_section_recent_activity)
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
    if (uiState.recentActivity.isNotEmpty()) {
        TextButton(onClick = onOpenAuditLog, modifier = Modifier.padding(horizontal = ButtonSpacing)) {
            Text(stringResource(R.string.admin_view_all_activity))
        }
    }
}

@Composable
private fun ManageRow(icon: @Composable () -> Unit, @StringRes title: Int, summary: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(summary) },
    )
}

@Composable
private fun SectionHeading(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = ContentPadding, end = ContentPadding, top = ContentPadding)
            .semantics { heading() },
    )
}
