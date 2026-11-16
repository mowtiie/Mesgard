package com.mowtiie.mesgard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Shows the stored account at the top of Settings and refreshes it once. A failed refresh keeps what is stored, and the
 * Account screen reports the failure when the person opens it.
 */
@HiltViewModel
internal class AccountSummaryViewModel @Inject constructor(private val repository: AccountRepository) : ViewModel() {
    val account: StateFlow<Account?> = repository.observeAccount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = null,
    )

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
