package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.RuleSummaries
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.ui.admin.groups.labelResource
import com.mowtiie.mesgard.ui.common.NavigationChevron
import com.mowtiie.mesgard.ui.common.SectionHeader
import java.time.format.DateTimeFormatter

private const val DAYS_IN_YEAR = 365
private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm")

/** The admin screens, in groups: who is in the organization, how it is structured, its rules and its oversight. */
@Composable
internal fun ManageSections(uiState: AdministrationUiState, summaries: RuleSummaries, navigation: AdminNavigation) {
    SectionHeader(R.string.admin_section_people)
    ManageRow(
        icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
        title = R.string.members_title,
        summary = membersSummary(uiState),
        onClick = navigation.onOpenMembers,
    )
    ManageRow(
        icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
        title = R.string.former_members_title,
        summary = stringResource(R.string.settings_former_members_summary),
        onClick = navigation.onOpenFormerMembers,
    )
    SectionHeader(R.string.admin_section_structure)
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_group_outline), contentDescription = null) },
        title = R.string.groups_title,
        summary = uiState.groupCount?.let { pluralStringResource(R.plurals.admin_groups_count, it, it) }
            ?: stringResource(R.string.settings_groups_summary),
        onClick = navigation.onOpenGroups,
    )
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_group_outline), contentDescription = null) },
        title = R.string.group_creation_title,
        summary = summaries.groupCreation?.let { stringResource(it.labelResource()) }
            ?: stringResource(R.string.settings_group_creation_summary),
        onClick = navigation.onOpenGroupCreation,
    )
    RulesSection(summaries, navigation)
    OversightSection(navigation)
}

@Composable
private fun RulesSection(summaries: RuleSummaries, navigation: AdminNavigation) {
    SectionHeader(R.string.admin_section_rules)
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_chat_outline), contentDescription = null) },
        title = R.string.messaging_rules_title,
        summary = rulesSummary(summaries.messagingRules),
        onClick = navigation.onOpenMessagingRules,
    )
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_schedule), contentDescription = null) },
        title = R.string.retention_title,
        summary = retentionSummary(summaries),
        onClick = navigation.onOpenRetention,
    )
    ManageRow(
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = R.string.legal_holds_title,
        summary = holdsSummary(summaries.activeHoldCount),
        onClick = navigation.onOpenLegalHolds,
    )
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_description), contentDescription = null) },
        title = R.string.privacy_notice_settings_title,
        summary = stringResource(R.string.settings_privacy_notice_summary),
        onClick = navigation.onOpenPrivacyNoticeSettings,
    )
}

@Composable
private fun OversightSection(navigation: AdminNavigation) {
    SectionHeader(R.string.admin_section_oversight)
    ManageRow(
        icon = { Icon(painterResource(R.drawable.ic_flag), contentDescription = null) },
        title = R.string.report_reviewers_title,
        summary = stringResource(R.string.settings_report_reviewers_summary),
        onClick = navigation.onOpenReportReviewers,
    )
    ManageRow(
        icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
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

@Composable
private fun retentionSummary(summaries: RuleSummaries): String {
    val retention = summaries.retention
    val days = retention?.days
    return when {
        retention == null -> stringResource(R.string.settings_retention_summary)
        days == null -> stringResource(R.string.admin_retention_none)
        days % DAYS_IN_YEAR == 0 -> pluralStringResource(
            R.plurals.admin_retention_years,
            days / DAYS_IN_YEAR,
            days / DAYS_IN_YEAR,
        )
        else -> pluralStringResource(R.plurals.admin_retention_days, days, days)
    }
}

@Composable
private fun rulesSummary(rules: MessagingRules?): String {
    if (rules == null) return stringResource(R.string.settings_messaging_rules_summary)
    val parts = buildList {
        if (rules.blockStaffStudentDirectMessages) add(stringResource(R.string.admin_rules_no_direct))
        val start = rules.quietStart
        val end = rules.quietEnd
        if (start != null && end != null) {
            add(stringResource(R.string.admin_rules_quiet, ClockFormat.format(start), ClockFormat.format(end)))
        }
    }
    return if (parts.isEmpty()) stringResource(R.string.admin_rules_none) else parts.joinToString(". ")
}

@Composable
private fun holdsSummary(count: Int?): String = when (count) {
    null -> stringResource(R.string.settings_legal_holds_summary)
    0 -> stringResource(R.string.admin_holds_none)
    else -> pluralStringResource(R.plurals.admin_holds_count, count, count)
}

@Composable
private fun ManageRow(icon: @Composable () -> Unit, @StringRes title: Int, summary: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(summary) },
        trailingContent = { NavigationChevron() },
    )
}
