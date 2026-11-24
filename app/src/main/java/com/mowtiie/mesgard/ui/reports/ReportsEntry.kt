package com.mowtiie.mesgard.ui.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.settings.SettingsCategoryRow

/** The way into the reports, shown only to a reviewer. */
@Composable
internal fun ReportsRow(onOpen: () -> Unit) {
    SettingsCategoryRow(
        icon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
        title = R.string.reports_title,
        summary = stringResource(R.string.settings_reports_summary),
        onClick = onOpen,
    )
}
