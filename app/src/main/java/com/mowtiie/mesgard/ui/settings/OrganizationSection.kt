package com.mowtiie.mesgard.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.SectionHeader
import com.mowtiie.mesgard.ui.reports.ReportsEntryViewModel
import com.mowtiie.mesgard.ui.reports.ReportsRow

/** The duties a person has for their organization. Nothing shows, not even the header, for an ordinary member. */
@Composable
internal fun OrganizationSection(
    navigation: SettingsNavigation,
    administrationViewModel: AdministrationEntryViewModel = hiltViewModel(),
    reportsViewModel: ReportsEntryViewModel = hiltViewModel(),
) {
    val isAdmin by administrationViewModel.isAdmin.collectAsStateWithLifecycle()
    val isReviewer by reportsViewModel.isReviewer.collectAsStateWithLifecycle()
    if (!isAdmin && !isReviewer) return
    SectionHeader(R.string.settings_section_organization)
    if (isAdmin) AdministrationRow(navigation.onOpenAdministration)
    if (isReviewer) ReportsRow(navigation.onOpenReports)
}
