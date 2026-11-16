package com.mowtiie.mesgard.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
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
internal class AccountViewModel @Inject constructor(private val repository: AccountRepository) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<AccountUiState> = combine(repository.observeAccount(), localState) { account, local ->
        AccountUiState(
            account = account,
            isRefreshing = local.isRefreshing,
            showSignOutConfirmation = local.showSignOutConfirmation,
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

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private data class LocalState(
        val isRefreshing: Boolean = true,
        val showSignOutConfirmation: Boolean = false,
        val error: AccountError? = null,
    )
}

private fun RefreshError.toAccountError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> AccountError.REFRESH_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> AccountError.REFRESH_UNKNOWN
}
