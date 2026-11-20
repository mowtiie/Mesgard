package com.mowtiie.mesgard.ui.contacts

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.PreferenceError

internal enum class ContactsError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
    START_NETWORK_UNAVAILABLE,
    START_UNKNOWN,
}

/**
 * [contacts] holds the people that match [query], and nobody while it is blank: people are found by searching, not
 * by scrolling a roster. While it is blank the list offers the people the user chatted with ([recent]), the
 * [favorites] and the [groups] the user can start. Typing filters the groups too. The totals count them without the
 * filter.
 */
internal data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val recent: List<Contact> = emptyList(),
    val favorites: List<Contact> = emptyList(),
    val groups: List<Group> = emptyList(),
    val totalContacts: Int = 0,
    val totalGroups: Int = 0,
    val query: String = "",
    val onlineUserIds: Set<String> = emptySet(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    /** True while the server has not yet answered the text typed, so the list may still change. */
    val isSearching: Boolean = false,
    /** The server did not answer the search, so the people listed are the ones stored on this phone. */
    val isShowingSavedPeople: Boolean = false,
    /** The server stopped at its limit, so more people may match the text. */
    val mayHaveMoreResults: Boolean = false,
    val isStartingConversation: Boolean = false,
    val conversationToOpen: String? = null,
    val error: ContactsError? = null,
    val favoriteError: PreferenceError? = null,
) {
    val isFiltering: Boolean
        get() = query.isNotBlank()

    val showSearch: Boolean
        get() = totalContacts > 0 || totalGroups > 0 || isFiltering

    val showRecent: Boolean
        get() = !isFiltering && recent.isNotEmpty()

    /** While nothing is typed, the people on the favorites list are shown, so the common ones are one tap away. */
    val showFavorites: Boolean
        get() = !isFiltering && favorites.isNotEmpty()

    val showPeopleHeader: Boolean
        get() = isFiltering && contacts.isNotEmpty()

    val showSavedPeopleNote: Boolean
        get() = showPeopleHeader && isShowingSavedPeople

    val showMoreResultsNote: Boolean
        get() = showPeopleHeader && mayHaveMoreResults

    /** Nobody and no group to offer yet, so the screen asks for a search. */
    val showSearchPrompt: Boolean
        get() = isLoaded &&
            totalContacts > 0 &&
            !isFiltering &&
            recent.isEmpty() &&
            favorites.isEmpty() &&
            groups.isEmpty()

    val showEmptyMessage: Boolean
        get() = isLoaded && totalContacts == 0 && totalGroups == 0 && !isRefreshing

    val showNoResults: Boolean
        get() = isFiltering && contacts.isEmpty() && groups.isEmpty()
}
