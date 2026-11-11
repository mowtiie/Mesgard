package com.mowtiie.mesgard.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
internal class LockViewModel @Inject constructor(
    private val appLock: AppLock,
    private val deviceSettings: DeviceSettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun onAuthenticated() = appLock.unlock()

    fun onAuthenticationUnavailable() {
        deviceSettings.setAppLockEnabled(false)
        appLock.unlock()
        viewModelScope.launch { authRepository.signOut() }
    }
}
