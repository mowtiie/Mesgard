package com.mowtiie.mesgard.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.MfaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val mfaRepository: MfaRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<AccountUiState> = combine(
        repository.observeAccount(),
        mfaRepository.isEnabled,
        localState,
    ) { account, isMfaEnabled, local ->
        AccountUiState(
            account = account,
            isRefreshing = local.isRefreshing,
            showSignOutConfirmation = local.showSignOutConfirmation,
            mfa = mfaRowState(account, isMfaEnabled),
            showDisableMfaConfirmation = local.showDisableMfaConfirmation,
            isWorking = local.isWorking,
            error = local.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = AccountUiState(isRefreshing = true),
    )

    init {
        viewModelScope.launch {
            val result = repository.refresh()
            localState.update {
                it.copy(isRefreshing = false, error = (result as? RefreshResult.Failure)?.error?.toAccountError())
            }
        }
    }

    fun onSignOutClicked() = localState.update { it.copy(showSignOutConfirmation = true) }

    fun onSignOutDismissed() = localState.update { it.copy(showSignOutConfirmation = false) }

    fun onDisableMfaClicked() = localState.update { it.copy(showDisableMfaConfirmation = true) }

    fun onDisableMfaDismissed() = localState.update { it.copy(showDisableMfaConfirmation = false) }

    fun onDisableMfaConfirmed() {
        if (localState.value.isWorking) return
        localState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val failure = (mfaRepository.disable() as? MfaResult.Failure)?.error
            localState.update {
                it.copy(
                    isWorking = false,
                    showDisableMfaConfirmation = failure != null && it.showDisableMfaConfirmation,
                    error = failure?.toAccountError(),
                )
            }
        }
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private data class LocalState(
        val isRefreshing: Boolean = true,
        val showSignOutConfirmation: Boolean = false,
        val showDisableMfaConfirmation: Boolean = false,
        val isWorking: Boolean = false,
        val error: AccountError? = null,
    )
}

/** Administrators and staff must keep two-step verification on, so they can set it up but not turn it off. */
private fun mfaRowState(account: Account?, isEnabled: Boolean): MfaRowState = when {
    !isEnabled -> MfaRowState.OFF
    account?.memberships.orEmpty().any { it.role == OrgRole.ORG_ADMIN || it.role == OrgRole.STAFF } ->
        MfaRowState.REQUIRED
    else -> MfaRowState.ON
}

private fun RefreshError.toAccountError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> AccountError.REFRESH_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> AccountError.REFRESH_UNKNOWN
}

private fun MfaError.toAccountError() = when (this) {
    MfaError.NETWORK_UNAVAILABLE -> AccountError.MFA_NETWORK_UNAVAILABLE
    else -> AccountError.MFA_UNKNOWN
}
