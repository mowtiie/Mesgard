package com.mowtiie.mesgard.ui.contacts

import com.mowtiie.mesgard.domain.model.Contact

internal data class ContactsSelectionCallbacks(
    val onToggled: (Contact) -> Unit,
    val onSelectAll: () -> Unit,
    val onClose: () -> Unit,
    val onMessage: (Contact) -> Unit,
    val onFavorite: () -> Unit,
    val onErrorShown: () -> Unit,
)

/** The selection state and what can be done with it, passed together to the lists. */
internal data class ContactsSelection(
    val state: ContactsSelectionUiState = ContactsSelectionUiState(),
    val callbacks: ContactsSelectionCallbacks = ContactsSelectionCallbacks({}, {}, {}, {}, {}, {}),
)
