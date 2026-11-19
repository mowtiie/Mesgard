package com.mowtiie.mesgard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.repository.ContactRepository
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

internal fun Contact.selectionKey() = "$orgId:$userId"

internal data class ContactsSelectionUiState(
    val selected: List<Contact> = emptyList(),
    val error: PreferenceError? = null,
) {
    val isActive: Boolean
        get() = selected.isNotEmpty()

    /** Message is offered for one person, because a conversation starts with one person at a time. */
    val single: Contact?
        get() = selected.singleOrNull()

    /** Favorite for everyone, unless everyone selected already is a favorite. */
    val removesFavorites: Boolean
        get() = selected.isNotEmpty() && selected.all { it.isFavorite }

    fun isSelected(contact: Contact) = selected.any { it.selectionKey() == contact.selectionKey() }
}

/** A long press selects people; the bar then messages one person or favorites everyone selected. */
@HiltViewModel
internal class ContactsSelectionViewModel @Inject constructor(private val repository: ContactRepository) :
    ViewModel() {
    private val selectedKeys = MutableStateFlow(emptySet<String>())
    private val error = MutableStateFlow<PreferenceError?>(null)

    val uiState: StateFlow<ContactsSelectionUiState> = combine(
        repository.observeContacts(),
        selectedKeys,
        error,
    ) { contacts, keys, currentError ->
        ContactsSelectionUiState(contacts.filter { it.selectionKey() in keys }, currentError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ContactsSelectionUiState(),
    )

    fun onToggled(contact: Contact) = selectedKeys.update { keys ->
        val key = contact.selectionKey()
        if (key in keys) keys - key else keys + key
    }

    fun onSelectAll(contacts: Collection<Contact>) {
        selectedKeys.value = contacts.mapTo(mutableSetOf()) { it.selectionKey() }
    }

    fun onSelectionClosed() {
        selectedKeys.value = emptySet()
    }

    fun onFavoriteClicked() {
        val state = uiState.value
        if (!state.isActive) return
        selectedKeys.value = emptySet()
        viewModelScope.launch {
            val result = repository.setFavorite(state.selected.toSet(), favorite = !state.removesFavorites)
            if (result is PreferenceResult.Failure) error.value = result.error
        }
    }

    fun onErrorShown() {
        error.value = null
    }
}
