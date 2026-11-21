package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.BuildConfig
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.ui.account.SignOutDialog
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.reports.ReportsEntry
import com.mowtiie.mesgard.ui.theme.IsDynamicColorSupported

@Composable
internal fun SettingsScreenRoute(
    navigation: SettingsNavigation,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        navigation = navigation,
        onSignOut = onSignOut,
        onBack = onBack,
        modifier = modifier,
    )
}

/** The home of Settings: the account, then one row for each page, each showing what is currently set. */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    navigation: SettingsNavigation,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSignOutConfirmationShown by rememberSaveable { mutableStateOf(false) }

    TitledScreen(
        title = stringResource(R.string.settings_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
    ) { padding ->
        ScrollingPage(padding) {
            AccountSummary(navigation.onOpenAccount)
            AdministrationEntry(navigation.onOpenAdministration)
            ReportsEntry(navigation.onOpenReports)
            SettingsCategoryRow(
                icon = { Icon(painterResource(R.drawable.ic_palette), contentDescription = null) },
                title = R.string.settings_appearance,
                summary = appearanceSummary(uiState.settings),
                onClick = navigation.onOpenAppearance,
            )
            SettingsCategoryRow(
                icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                title = R.string.settings_notifications_title,
                summary = notificationsSummary(uiState.pushDistributorName),
                onClick = navigation.onOpenNotifications,
            )
            SettingsCategoryRow(
                icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                title = R.string.settings_privacy_security,
                summary = stringResource(securitySummary(uiState.settings).text),
                onClick = navigation.onOpenSecurity,
            )
            SettingsCategoryRow(
                icon = { Icon(painterResource(R.drawable.ic_description), contentDescription = null) },
                title = R.string.privacy_notice_title,
                summary = stringResource(R.string.privacy_notice_summary),
                onClick = navigation.onOpenPrivacyNotice,
            )
            SettingsCategoryRow(
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                title = R.string.about_title,
                summary = stringResource(R.string.settings_about_summary, BuildConfig.VERSION_NAME),
                onClick = navigation.onOpenAbout,
            )
            HorizontalDivider()
            SignOutRow { isSignOutConfirmationShown = true }
        }
    }

    if (isSignOutConfirmationShown) {
        SignOutDialog(
            onConfirm = {
                isSignOutConfirmationShown = false
                onSignOut()
            },
            onDismiss = { isSignOutConfirmationShown = false },
        )
    }
}

@Composable
private fun appearanceSummary(settings: DeviceSettings): String {
    val theme = stringResource(settings.themeMode.labelResource())
    return if (settings.dynamicColor && IsDynamicColorSupported) {
        stringResource(R.string.settings_appearance_summary_dynamic, theme)
    } else {
        theme
    }
}

@Composable
private fun notificationsSummary(distributorName: String?): String = if (distributorName == null) {
    stringResource(R.string.settings_notifications_summary_not_set_up)
} else {
    stringResource(R.string.settings_notifications_summary_using, distributorName)
}

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.sign_out_button)) },
    )
}
