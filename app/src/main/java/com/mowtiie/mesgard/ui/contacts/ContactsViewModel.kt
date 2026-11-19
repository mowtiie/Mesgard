package com.mowtiie.mesgard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ContactSearchResult
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupRepository
import com.mowtiie.mesgard.ui.common.matchesQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
internal const val SEARCH_PAUSE_MS = 300L

@HiltViewModel
internal class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,
    presenceTracker: PresenceTracker,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    /** What the server found for the text typed, after a pause. Until it answers, the stored roster is searched. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val serverSearch: Flow<ServerSearch> = localState
        .map { it.query.trim() }
        .distinctUntilChanged()
        .debounce(SEARCH_PAUSE_MS)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(ServerSearch())
            } else {
                flow {
                    emit(ServerSearch(query))
                    emit(
                        ServerSearch(
                            query,
                            (contactRepository.search(query) as? ContactSearchResult.Found)?.contacts,
                            false,
                        ),
                    )
                }
            }
        }
        .onStart { emit(ServerSearch()) }

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.observeContacts(),
        groupRepository.observeGroups(),
        presenceTracker.onlineUserIds,
        localState,
        serverSearch,
    ) { allContacts, allGroups, onlineUserIds, local, server ->
        val startableGroups = allGroups.filter { it.canStartConversation }
        val query = local.query.trim()
        val serverAnswered = server.query == query && server.contacts != null
        ContactsUiState(
            contacts = when {
                query.isEmpty() -> emptyList()
                serverAnswered -> server.contacts.orEmpty()
                else -> allContacts.filter { matchesQuery(it.displayName, query) }
            },
            isSearching = query.isNotEmpty() && !serverAnswered && (server.query != query || server.isSearching),
            favorites = allContacts.filter { it.isFavorite },
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

    private data class ServerSearch(
        val query: String = "",
        val contacts: List<Contact>? = null,
        val isSearching: Boolean = true,
    )

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
