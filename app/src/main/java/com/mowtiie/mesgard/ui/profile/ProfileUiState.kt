package com.mowtiie.mesgard.ui.profile

import com.mowtiie.mesgard.domain.model.Account

internal enum class ProfileError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
}

internal data class ProfileUiState(
    val account: Account? = null,
    val isRefreshing: Boolean = false,
    val showSignOutConfirmation: Boolean = false,
    val error: ProfileError? = null,
)
