package com.mowtiie.mesgard.ui.account

import com.mowtiie.mesgard.domain.model.Account

internal enum class AccountError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
    MFA_NETWORK_UNAVAILABLE,
    MFA_UNKNOWN,
}

/** [REQUIRED] is for administrators and staff, who cannot turn it off. */
internal enum class MfaRowState {
    OFF,
    ON,
    REQUIRED,
}

internal data class AccountUiState(
    val account: Account? = null,
    val isRefreshing: Boolean = false,
    val showSignOutConfirmation: Boolean = false,
    val mfa: MfaRowState = MfaRowState.OFF,
    val showDisableMfaConfirmation: Boolean = false,
    val isWorking: Boolean = false,
    val error: AccountError? = null,
)
