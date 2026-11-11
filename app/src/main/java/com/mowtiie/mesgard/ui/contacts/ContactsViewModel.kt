package com.mowtiie.mesgard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupRepository
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
internal class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,
    presenceTracker: PresenceTracker,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.observeContacts(),
        groupRepository.observeGroups(),
        presenceTracker.onlineUserIds,
        localState,
    ) { allContacts, allGroups, onlineUserIds, local ->
        val startableGroups = allGroups.filter { it.canStartConversation }
        ContactsUiState(
            contacts = allContacts.filter { matchesQuery(it.displayName, local.query) },
            groups = startableGroups.filter { matchesQuery(it.name, local.query) },
            totalContacts = allContacts.size,
            totalGroups = startableGroups.size,
            query = local.query,
            onlineUserIds = onlineUserIds,
            selectedSection = local.selectedSection,
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
            val failure = listOf(contactRepository.refresh(), groupRepository.refresh())
                .filterIsInstance<RefreshResult.Failure>()
                .firstOrNull()
            localState.update { it.copy(isRefreshing = false, error = failure?.error?.toRefreshError()) }
        }
    }

    fun onSectionSelected(section: ContactsSection) = localState.update { it.copy(selectedSection = section) }

    fun onQueryChanged(query: String) = localState.update { it.copy(query = query) }

    fun onContactClicked(contact: Contact) = startConversation { contactRepository.startDirectConversation(contact) }

    fun onGroupClicked(group: Group) = startConversation { groupRepository.startGroupConversation(group) }

    fun onConversationOpened() = localState.update { it.copy(conversationToOpen = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private fun startConversation(start: suspend () -> StartConversationResult) {
        if (localState.value.isStartingConversation) return
        localState.update { it.copy(isStartingConversation = true) }
        viewModelScope.launch {
            when (val result = start()) {
                is StartConversationResult.Started ->
                    localState.update {
                        it.copy(isStartingConversation = false, conversationToOpen = result.conversationId)
                    }
                is StartConversationResult.Failure ->
                    localState.update { it.copy(isStartingConversation = false, error = result.error.toStartError()) }
            }
        }
    }

    private data class LocalState(
        val selectedSection: ContactsSection = ContactsSection.PEOPLE,
        val query: String = "",
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

internal fun matchesQuery(name: String, query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.isEmpty() || name.contains(trimmed, ignoreCase = true)
}
