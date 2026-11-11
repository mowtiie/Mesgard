package com.mowtiie.mesgard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.LockTimeout
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.push.PushRegistration
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import com.mowtiie.mesgard.domain.repository.NotificationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val deviceSettings: DeviceSettingsRepository,
    private val notificationSettings: NotificationSettingsRepository,
    private val pushRegistration: PushRegistration,
) : ViewModel() {
    private val error = MutableStateFlow<SettingsError?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        deviceSettings.settings,
        pushRegistration.distributorName,
        error,
    ) { settings, distributorName, currentError ->
        SettingsUiState(settings = settings, pushDistributorName = distributorName, error = currentError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = SettingsUiState(
            settings = deviceSettings.settings.value,
            pushDistributorName = pushRegistration.distributorName.value,
        ),
    )

    init {
        viewModelScope.launch { notificationSettings.refresh() }
    }

    fun onScreenPrivacyChanged(enabled: Boolean) = deviceSettings.setScreenPrivacyEnabled(enabled)

    fun onAppLockChanged(enabled: Boolean) = deviceSettings.setAppLockEnabled(enabled)

    fun onPushDistributorUnavailable() {
        error.value = SettingsError.PUSH_DISTRIBUTOR_UNAVAILABLE
    }

    fun onAppLockUnavailable() {
        error.value = SettingsError.DEVICE_LOCK_REQUIRED
    }

    fun onLockTimeoutSelected(timeout: LockTimeout) = deviceSettings.setLockTimeout(timeout)

    fun onNotificationPreviewSelected(preview: NotificationPreview) {
        viewModelScope.launch {
            val result = notificationSettings.setPreview(preview)
            if (result is RefreshResult.Failure) error.value = result.error.toPreviewError()
        }
    }

    fun onPushDistributorChosen() = pushRegistration.onDistributorChosen()

    fun onErrorShown() {
        error.value = null
    }
}

private fun RefreshError.toPreviewError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> SettingsError.PREVIEW_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> SettingsError.PREVIEW_UNKNOWN
}
