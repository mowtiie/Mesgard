package com.mowtiie.mesgard.ui.contacts

import com.mowtiie.mesgard.domain.model.Contact

internal enum class ContactsError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
    START_NETWORK_UNAVAILABLE,
    START_UNKNOWN,
}

internal data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStartingConversation: Boolean = false,
    val conversationToOpen: String? = null,
    val error: ContactsError? = null,
) {
    val showEmptyMessage: Boolean
        get() = isLoaded && contacts.isEmpty() && !isRefreshing
}
