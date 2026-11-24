package com.mowtiie.mesgard.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ContactSearchResult
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
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
internal const val RECENT_LIMIT = 5

// More than the list shows, because the favorites among them are taken out.
private const val RECENT_QUERY_LIMIT = RECENT_LIMIT * 2

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
                    val found = contactRepository.search(query) as? ContactSearchResult.Found
                    emit(
                        ServerSearch(
                            query,
                            found?.contacts,
                            isSearching = false,
                            mayHaveMore =
                            found?.mayHaveMore == true,
                        ),
                    )
                }
            }
        }
        .onStart { emit(ServerSearch()) }

    private val people = combine(
        contactRepository.observeContacts(),
        contactRepository.observeRecentContacts(RECENT_QUERY_LIMIT),
    ) { roster, recentlyChatted -> roster to recentlyChatted }

    val uiState: StateFlow<ContactsUiState> = combine(
        people,
        groupRepository.observeGroups(),
        presenceTracker.onlineUserIds,
        localState,
        serverSearch,
    ) { (allContacts, recentlyChatted), allGroups, onlineUserIds, local, server ->
        val startableGroups = allGroups.filter { it.canStartConversation }
        val query = local.query.trim()
        val serverAnswered = server.query == query && server.contacts != null
        val isSearching = query.isNotEmpty() && !serverAnswered && (server.query != query || server.isSearching)
        val favoriteIds = allContacts.filter { it.isFavorite }.mapTo(mutableSetOf()) { it.userId }
        ContactsUiState(
            contacts = when {
                query.isEmpty() -> emptyList()
                // A person found by searching is marked from the roster, so the star follows a change at once.
                serverAnswered -> server.contacts.orEmpty().map { it.copy(isFavorite = it.userId in favoriteIds) }
                else -> allContacts.filter { it.matchesQuery(query) }
            },
            isSearching = isSearching,
            // The server was asked and did not answer, so what is listed comes from the stored roster.
            isShowingSavedPeople = query.isNotEmpty() && !serverAnswered && !isSearching,
            mayHaveMoreResults = serverAnswered && server.mayHaveMore,
            // A favorite is listed under Favorites, so it is not offered twice.
            recent = recentlyChatted.filter { !it.isFavorite }.take(RECENT_LIMIT),
            favorites = allContacts.filter { it.isFavorite },
            groups = startableGroups.filter { matchesQuery(it.name, local.query) },
            totalContacts = allContacts.size,
            totalGroups = startableGroups.size,
            query = local.query,
            onlineUserIds = onlineUserIds,
            isLoaded = true,
            isRefreshing = local.isRefreshing,
            isStartingConversation = local.isStartingConversation,
            conversationToOpen = local.conversationToOpen,
            error = local.error,
            favoriteError = local.favoriteError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ContactsUiState(),
    )

    init {
        refreshWith { contactRepository.refreshIfStale() }
    }

    /** A pull to refresh asks for the whole roster. Opening the screen asks only for what is stale. */
    fun onRefresh() = refreshWith { contactRepository.refresh() }

    private fun refreshWith(refreshRoster: suspend () -> RefreshResult) {
        if (localState.value.isRefreshing) return
        localState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val failure = listOf(refreshRoster(), groupRepository.refresh())
                .filterIsInstance<RefreshResult.Failure>()
                .firstOrNull()
            localState.update { it.copy(isRefreshing = false, error = failure?.error?.toRefreshError()) }
        }
    }

    fun onQueryChanged(query: String) = localState.update { it.copy(query = query) }

    fun onContactClicked(contact: Contact) = startConversation { contactRepository.startDirectConversation(contact) }

    fun onGroupClicked(group: Group) = startConversation { groupRepository.startGroupConversation(group) }

    fun onConversationOpened() = localState.update { it.copy(conversationToOpen = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    /** Adds the person to the favorites, or takes them out when they are one. The phone shows it at once. */
    fun onFavoriteToggled(contact: Contact) {
        viewModelScope.launch {
            val result = contactRepository.setFavorite(setOf(contact), favorite = !contact.isFavorite)
            if (result is PreferenceResult.Failure) localState.update { it.copy(favoriteError = result.error) }
        }
    }

    fun onFavoriteErrorShown() = localState.update { it.copy(favoriteError = null) }

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
        val mayHaveMore: Boolean = false,
    )

    private data class LocalState(
        val query: String = "",
        val isRefreshing: Boolean = false,
        val isStartingConversation: Boolean = false,
        val conversationToOpen: String? = null,
        val error: ContactsError? = null,
        val favoriteError: PreferenceError? = null,
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
