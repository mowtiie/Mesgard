package com.mowtiie.mesgard.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.ui.common.SectionHeader
import com.mowtiie.mesgard.ui.lock.AuthAvailability
import com.mowtiie.mesgard.ui.lock.AuthOutcome
import com.mowtiie.mesgard.ui.lock.BiometricAuthenticator
import kotlinx.coroutines.launch

@Composable
internal fun SecuritySettingsScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
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

    SettingsSubpage(
        title = R.string.settings_privacy_security,
        onBack = onBack,
        modifier = modifier,
        error = uiState.error,
        onErrorShown = viewModel::onErrorShown,
    ) {
        SwitchRow(
            title = R.string.settings_screen_privacy,
            summary = R.string.settings_screen_privacy_summary,
            checked = uiState.settings.screenPrivacyEnabled,
            onCheckedChange = viewModel::onScreenPrivacyChanged,
        )
        OnlineStatusRow()
        SwitchRow(
            title = R.string.settings_app_lock,
            summary = R.string.settings_app_lock_summary,
            checked = uiState.settings.appLockEnabled,
            onCheckedChange = ::onAppLockToggled,
        )
        if (uiState.settings.appLockEnabled) {
            SectionHeader(R.string.settings_lock_timeout)
            RadioGroup(
                options = LockTimeout.entries.map { it to RadioLabel(it.labelResource()) },
                selected = uiState.settings.lockTimeout,
                onSelected = viewModel::onLockTimeoutSelected,
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
