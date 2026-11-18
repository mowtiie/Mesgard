package com.mowtiie.mesgard.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.BuildConfig
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.server.AccessKeyProblem
import com.mowtiie.mesgard.domain.server.AppRestarter
import com.mowtiie.mesgard.domain.server.ServerAddress
import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import com.mowtiie.mesgard.domain.server.findAccessKeyProblem
import com.mowtiie.mesgard.domain.server.parseServerAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class ServerSetupViewModel @Inject constructor(
    private val store: ServerConfigStore,
    private val checker: ServerChecker,
    private val restarter: AppRestarter,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ServerSetupUiState(
            address = store.current?.url.orEmpty(),
            accessKey = store.current?.anonKey.orEmpty(),
            canCancel = store.current != null,
        ),
    )
    val uiState: StateFlow<ServerSetupUiState> = mutableUiState.asStateFlow()

    /** A setup link only fills the form. The person still has to look at the address and connect. */
    fun onPrefill(prefill: ServerSetupPrefill) = mutableUiState.update {
        it.copy(address = prefill.address, accessKey = prefill.accessKey, isFromLink = true, error = null)
    }

    /** The saved server failed the check at start, so the form shows what went wrong. */
    fun onStartupProblem(problem: ServerSetupError) = mutableUiState.update {
        it.copy(error = problem, hasStartupProblem = true)
    }

    fun onAddressChanged(address: String) = mutableUiState.update { it.copy(address = address, error = null) }

    fun onAccessKeyChanged(accessKey: String) = mutableUiState.update { it.copy(accessKey = accessKey, error = null) }

    fun onFinishedHandled() = mutableUiState.update { it.copy(isFinished = false) }

    fun onConnectClicked() {
        val state = mutableUiState.value
        if (state.isChecking) return
        when (val validation = validate(state)) {
            is Validation.Failed -> mutableUiState.update { it.copy(error = validation.error) }
            is Validation.Ready -> connect(validation.config)
        }
    }

    private fun connect(config: ServerConfig) {
        mutableUiState.update { it.copy(isChecking = true, error = null) }
        viewModelScope.launch {
            val error = when (checker.check(config)) {
                ServerCheckResult.Reachable -> null
                ServerCheckResult.Unreachable -> ServerSetupError.UNREACHABLE
                ServerCheckResult.KeyRejected -> ServerSetupError.KEY_REJECTED
                ServerCheckResult.NotASupabaseServer -> ServerSetupError.NOT_A_SERVER
            }
            mutableUiState.update { it.copy(isChecking = false, error = error) }
            if (error == null) finish(config)
        }
    }

    private fun finish(config: ServerConfig) {
        if (config == store.current) {
            mutableUiState.update { it.copy(isFinished = true) }
        } else {
            store.save(config)
            restarter.restart()
        }
    }

    private fun validate(state: ServerSetupUiState): Validation {
        val address = parseServerAddress(state.address, allowCleartext = BuildConfig.DEBUG)
        val config = (address as? ServerAddress.Valid)?.let { ServerConfig(it.url, state.accessKey.trim()) }
        val error = addressError(address) ?: keyError(state.accessKey) ?: config?.let(::signOutError)
        return if (error == null && config != null) Validation.Ready(config) else Validation.Failed(error)
    }

    /** Local data and the session belong to the current server, so another one is only set up after signing out. */
    private fun signOutError(config: ServerConfig): ServerSetupError? =
        ServerSetupError.SIGN_OUT_FIRST.takeIf { authRepository.currentUserId != null && config != store.current }

    private sealed interface Validation {
        data class Ready(val config: ServerConfig) : Validation

        data class Failed(val error: ServerSetupError?) : Validation
    }
}

private fun addressError(address: ServerAddress): ServerSetupError? = when (address) {
    ServerAddress.Empty -> ServerSetupError.ADDRESS_EMPTY
    ServerAddress.Invalid -> ServerSetupError.ADDRESS_INVALID
    ServerAddress.CleartextNotAllowed -> ServerSetupError.ADDRESS_NOT_SECURE
    is ServerAddress.Valid -> null
}

private fun keyError(accessKey: String): ServerSetupError? = when (findAccessKeyProblem(accessKey)) {
    AccessKeyProblem.EMPTY -> ServerSetupError.KEY_EMPTY
    AccessKeyProblem.MALFORMED -> ServerSetupError.KEY_MALFORMED
    AccessKeyProblem.SECRET_KEY -> ServerSetupError.KEY_SECRET
    null -> null
}
