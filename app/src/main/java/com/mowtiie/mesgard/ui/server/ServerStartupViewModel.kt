package com.mowtiie.mesgard.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Checks the saved server, the one built into the app or chosen earlier, when the app starts and nobody is signed
 * in. A server that answers with the wrong key or is not a Supabase server sends the person to the setup screen with
 * the reason. An address that cannot be reached counts only when the device has a network, so working offline never
 * looks like a wrong server. Someone already signed in is never interrupted, because the app works from Room.
 */
@HiltViewModel
internal class ServerStartupViewModel @Inject constructor(
    private val store: ServerConfigStore,
    private val checker: ServerChecker,
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor,
) : ViewModel() {
    private val mutableProblem = MutableStateFlow<ServerSetupError?>(null)
    val problem: StateFlow<ServerSetupError?> = mutableProblem.asStateFlow()

    init {
        checkSavedServer()
    }

    fun onProblemHandled() {
        mutableProblem.value = null
    }

    private fun checkSavedServer() {
        val config = store.current ?: return
        viewModelScope.launch {
            val state = authRepository.authState.first { it !is AuthState.Loading }
            if (state is AuthState.SignedOut) mutableProblem.value = problemOf(checker.check(config))
        }
    }

    private suspend fun problemOf(result: ServerCheckResult): ServerSetupError? = when (result) {
        ServerCheckResult.Reachable -> null
        ServerCheckResult.KeyRejected -> ServerSetupError.KEY_REJECTED
        ServerCheckResult.NotASupabaseServer -> ServerSetupError.NOT_A_SERVER
        ServerCheckResult.Unreachable -> ServerSetupError.UNREACHABLE.takeIf { connectivityMonitor.isOnline.first() }
    }
}
