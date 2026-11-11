package com.mowtiie.mesgard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
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

@HiltViewModel
internal class ContactsViewModel @Inject constructor(private val repository: ContactRepository) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ContactsUiState> = combine(repository.observeContacts(), localState) { contacts, local ->
        ContactsUiState(
            contacts = contacts,
            isLoaded = true,
            isRefreshing = local.isRefreshing,
            isStartingConversation = local.isStartingConversation,
            conversationToOpen = local.conversationToOpen,
            error = local.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ContactsUiState(),
    )

    init {
        onRefresh()
    }

    fun onRefresh() {
        if (localState.value.isRefreshing) return
        localState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = repository.refresh()
            localState.update {
                it.copy(isRefreshing = false, error = (result as? RefreshResult.Failure)?.error?.toRefreshError())
            }
        }
    }

    fun onContactClicked(contact: Contact) {
        if (localState.value.isStartingConversation) return
        localState.update { it.copy(isStartingConversation = true) }
        viewModelScope.launch {
            when (val result = repository.startDirectConversation(contact)) {
                is StartConversationResult.Started ->
                    localState.update {
                        it.copy(isStartingConversation = false, conversationToOpen = result.conversationId)
                    }
                is StartConversationResult.Failure ->
                    localState.update { it.copy(isStartingConversation = false, error = result.error.toStartError()) }
            }
        }
    }

    fun onConversationOpened() = localState.update { it.copy(conversationToOpen = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private data class LocalState(
        val isRefreshing: Boolean = false,
        val isStartingConversation: Boolean = false,
        val conversationToOpen: String? = null,
        val error: ContactsError? = null,
    )
}

private fun RefreshError.toRefreshError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> ContactsError.REFRESH_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> ContactsError.REFRESH_UNKNOWN
}

private fun RefreshError.toStartError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> ContactsError.START_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> ContactsError.START_UNKNOWN
}
