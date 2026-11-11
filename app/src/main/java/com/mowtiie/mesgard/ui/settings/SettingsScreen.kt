package com.mowtiie.mesgard.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.lock.AuthAvailability
import com.mowtiie.mesgard.ui.lock.AuthOutcome
import com.mowtiie.mesgard.ui.lock.BiometricAuthenticator
import kotlinx.coroutines.launch

private val SectionHeaderPadding = 16.dp

@Composable
internal fun SettingsScreenRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    val promptTitle = stringResource(R.string.lock_prompt_title)

    fun onAppLockToggled(enabled: Boolean) {
        if (!enabled) {
            viewModel.onAppLockChanged(false)
            return
        }
        scope.launch {
            val canAuthenticate = BiometricAuthenticator.availability(context) == AuthAvailability.AVAILABLE
            if (!canAuthenticate || activity == null) {
                viewModel.onAppLockUnavailable()
            } else if (BiometricAuthenticator.authenticate(activity, promptTitle) == AuthOutcome.SUCCESS) {
                viewModel.onAppLockChanged(true)
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onScreenPrivacyChanged = viewModel::onScreenPrivacyChanged,
        onAppLockChanged = ::onAppLockToggled,
        onLockTimeoutSelected = viewModel::onLockTimeoutSelected,
        onNotificationPreviewSelected = viewModel::onNotificationPreviewSelected,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onScreenPrivacyChanged: (Boolean) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onLockTimeoutSelected: (LockTimeout) -> Unit,
    onNotificationPreviewSelected: (NotificationPreview) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(R.string.tab_settings),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader(R.string.settings_security)
            SwitchRow(
                title = R.string.settings_screen_privacy,
                summary = R.string.settings_screen_privacy_summary,
                checked = uiState.settings.screenPrivacyEnabled,
                onCheckedChange = onScreenPrivacyChanged,
            )
            SwitchRow(
                title = R.string.settings_app_lock,
                summary = R.string.settings_app_lock_summary,
                checked = uiState.settings.appLockEnabled,
                onCheckedChange = onAppLockChanged,
            )
            if (uiState.settings.appLockEnabled) {
                SectionHeader(R.string.settings_lock_timeout)
                RadioGroup(
                    options = LockTimeout.entries.map { it to RadioLabel(it.labelResource()) },
                    selected = uiState.settings.lockTimeout,
                    onSelected = onLockTimeoutSelected,
                )
            }
            SectionHeader(R.string.settings_notifications)
            RadioGroup(
                options = NotificationPreview.entries.map {
                    it to RadioLabel(it.labelResource(), it.summaryResource())
                },
                selected = uiState.settings.notificationPreview,
                onSelected = onNotificationPreviewSelected,
            )
        }
    }
}

private data class RadioLabel(@StringRes val title: Int, @StringRes val summary: Int? = null)

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = SectionHeaderPadding,
            end = SectionHeaderPadding,
            top = SectionHeaderPadding,
        ),
    )
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun <T> RadioGroup(options: List<Pair<T, RadioLabel>>, selected: T, onSelected: (T) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            ListItem(
                modifier = Modifier.selectable(
                    selected = value == selected,
                    role = Role.RadioButton,
                    onClick = { onSelected(value) },
                ),
                leadingContent = { RadioButton(selected = value == selected, onClick = null) },
                headlineContent = { Text(stringResource(label.title)) },
                supportingContent = label.summary?.let { summary -> { Text(stringResource(summary)) } },
            )
        }
    }
}

@StringRes
private fun LockTimeout.labelResource(): Int = when (this) {
    LockTimeout.IMMEDIATELY -> R.string.lock_timeout_immediately
    LockTimeout.ONE_MINUTE -> R.string.lock_timeout_one_minute
    LockTimeout.FIVE_MINUTES -> R.string.lock_timeout_five_minutes
}

@StringRes
private fun NotificationPreview.labelResource(): Int = when (this) {
    NotificationPreview.SENDER_AND_TEXT -> R.string.preview_sender_and_text
    NotificationPreview.SENDER_ONLY -> R.string.preview_sender_only
    NotificationPreview.HIDDEN -> R.string.preview_hidden
}

@StringRes
private fun NotificationPreview.summaryResource(): Int = when (this) {
    NotificationPreview.SENDER_AND_TEXT -> R.string.preview_sender_and_text_summary
    NotificationPreview.SENDER_ONLY -> R.string.preview_sender_only_summary
    NotificationPreview.HIDDEN -> R.string.preview_hidden_summary
}

@StringRes
private fun SettingsError.messageResource(): Int = when (this) {
    SettingsError.DEVICE_LOCK_REQUIRED -> R.string.settings_error_device_lock_required
    SettingsError.PREVIEW_NETWORK_UNAVAILABLE -> R.string.settings_error_preview_network_unavailable
    SettingsError.PREVIEW_UNKNOWN -> R.string.settings_error_preview_unknown
}
