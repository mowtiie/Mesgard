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

/**
 * [contacts] holds the people that match [query], and nobody while it is blank: people are found by searching, not
 * by scrolling a roster. [groups] are filtered by [query] too, and all shown while it is blank. The totals count them
 * without the filter.
 */
internal data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val groups: List<Group> = emptyList(),
    val totalContacts: Int = 0,
    val totalGroups: Int = 0,
    val query: String = "",
    val onlineUserIds: Set<String> = emptySet(),
    val selectedSection: ContactsSection = ContactsSection.PEOPLE,
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStartingConversation: Boolean = false,
    val conversationToOpen: String? = null,
    val error: ContactsError? = null,
) {
    val showSections: Boolean
        get() = totalGroups > 0

    val section: ContactsSection
        get() = if (showSections) selectedSection else ContactsSection.PEOPLE

    val isFiltering: Boolean
        get() = query.isNotBlank()

    val showSearch: Boolean
        get() = totalContacts > 0 || totalGroups > 0 || isFiltering

    val showSearchPrompt: Boolean
        get() = isLoaded && totalContacts > 0 && !isFiltering && section == ContactsSection.PEOPLE

    val showEmptyMessage: Boolean
        get() = isLoaded && totalContacts == 0 && !isRefreshing && section == ContactsSection.PEOPLE

    val showNoResults: Boolean
        get() = showNoResults(section)

    fun showNoResults(forSection: ContactsSection): Boolean = isFiltering &&
        when (forSection) {
            ContactsSection.PEOPLE -> contacts.isEmpty()
            ContactsSection.GROUPS -> groups.isEmpty()
        }
}
