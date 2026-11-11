package com.mowtiie.mesgard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.push.PushRegistration
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val realtimeSync: RealtimeSync,
    private val presenceTracker: PresenceTracker,
    private val pushRegistration: PushRegistration,
    private val appLock: AppLock,
    deviceSettings: DeviceSettingsRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = AuthState.Loading,
    )

    val isLocked: StateFlow<Boolean> = combine(appLock.isLocked, deviceSettings.settings) { locked, settings ->
        locked && settings.appLockEnabled
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = appLock.isLocked.value && deviceSettings.settings.value.appLockEnabled,
    )

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.SignedIn -> startRealtime()
                    AuthState.SignedOut -> stopWhenSignedOut()
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    private fun startRealtime() {
        realtimeSync.start(viewModelScope)
        presenceTracker.start(viewModelScope)
        viewModelScope.launch { pushRegistration.start() }
    }

    private fun stopWhenSignedOut() {
        realtimeSync.stop()
        presenceTracker.stop()
        appLock.unlock()
    }

    fun onSignOutClicked() {
        viewModelScope.launch {
            pushRegistration.stop()
            authRepository.signOut()
        }
    }
}
