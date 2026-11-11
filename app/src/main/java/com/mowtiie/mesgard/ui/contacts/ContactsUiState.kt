package com.mowtiie.mesgard.ui.contacts

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group

internal enum class ContactsError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
    START_NETWORK_UNAVAILABLE,
    START_UNKNOWN,
}

internal enum class ContactsSection {
    PEOPLE,
    GROUPS,
}

internal data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val groups: List<Group> = emptyList(),
    val selectedSection: ContactsSection = ContactsSection.PEOPLE,
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStartingConversation: Boolean = false,
    val conversationToOpen: String? = null,
    val error: ContactsError? = null,
) {
    val showSections: Boolean
        get() = groups.isNotEmpty()

    val section: ContactsSection
        get() = if (showSections) selectedSection else ContactsSection.PEOPLE

    val showEmptyMessage: Boolean
        get() = isLoaded && contacts.isEmpty() && !isRefreshing && section == ContactsSection.PEOPLE
}
