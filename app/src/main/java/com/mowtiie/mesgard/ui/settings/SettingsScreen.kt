package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.ui.account.SignOutDialog
import com.mowtiie.mesgard.ui.admin.AdminNavigation
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.theme.IsDynamicColorSupported

@Composable
internal fun SettingsScreenRoute(
    adminNavigation: AdminNavigation,
    navigation: SettingsNavigation,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        adminNavigation = adminNavigation,
        navigation = navigation,
        onSignOut = onSignOut,
        modifier = modifier,
    )
}

/** The home of Settings: the account, then one row for each page, each showing what is currently set. */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    adminNavigation: AdminNavigation,
    navigation: SettingsNavigation,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSignOutConfirmationShown by rememberSaveable { mutableStateOf(false) }

    TitledScreen(title = stringResource(R.string.tab_settings), modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            AccountSummary(navigation.onOpenAccount)
            AdministrationSection(adminNavigation)
            SettingsCategoryRow(
                icon = { Icon(painterResource(R.drawable.ic_palette), contentDescription = null) },
                title = R.string.settings_appearance,
                summary = appearanceSummary(uiState.settings),
                onClick = navigation.onOpenAppearance,
            )
            SettingsCategoryRow(
                icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                title = R.string.settings_notifications_title,
                summary = notificationsSummary(uiState.pushDistributorName),
                onClick = navigation.onOpenNotifications,
            )
            SettingsCategoryRow(
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                title = R.string.settings_privacy_security,
                summary = stringResource(securitySummary(uiState.settings).text),
                onClick = navigation.onOpenSecurity,
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
        leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.sign_out_button)) },
    )
}
