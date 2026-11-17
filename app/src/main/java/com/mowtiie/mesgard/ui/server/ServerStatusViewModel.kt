package com.mowtiie.mesgard.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.ServerStatus
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ServerStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Checks the server when the app opens, and again after sign in when the first answer could not be had. */
@HiltViewModel
internal class ServerStatusViewModel @Inject constructor(
    private val serverStatusRepository: ServerStatusRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val mutableStatus = MutableStateFlow(ServerStatus.UNKNOWN)
    val status: StateFlow<ServerStatus> = mutableStatus.asStateFlow()

    init {
        check()
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.SignedIn && mutableStatus.value == ServerStatus.UNKNOWN) check()
            }
        }
    }

    private fun check() {
        viewModelScope.launch { mutableStatus.value = serverStatusRepository.check() }
    }
}
