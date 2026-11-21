package com.mowtiie.mesgard.ui.reports

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.settings.SettingsCategoryRow

/** The way into the reports. Only a reviewer sees it. */
@Composable
internal fun ReportsEntry(onOpen: () -> Unit, viewModel: ReportsEntryViewModel = hiltViewModel()) {
    val isReviewer by viewModel.isReviewer.collectAsStateWithLifecycle()
    if (!isReviewer) return
    SettingsCategoryRow(
        icon = { Icon(painterResource(R.drawable.ic_flag), contentDescription = null) },
        title = R.string.reports_title,
        summary = stringResource(R.string.settings_reports_summary),
        onClick = onOpen,
    )
}
