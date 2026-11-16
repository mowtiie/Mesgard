package com.mowtiie.mesgard.ui.account

import com.mowtiie.mesgard.domain.model.Account

internal enum class AccountError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
}

internal data class AccountUiState(
    val account: Account? = null,
    val isRefreshing: Boolean = false,
    val showSignOutConfirmation: Boolean = false,
    val error: AccountError? = null,
)
