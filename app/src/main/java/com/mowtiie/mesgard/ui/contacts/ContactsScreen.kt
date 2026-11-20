package com.mowtiie.mesgard.ui.contacts

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.ui.chats.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NoResultsState
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen

@Composable
internal fun ContactsScreenRoute(
    onConversationStarted: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ContactsScreen(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onContactClicked = viewModel::onContactClicked,
        onFavoriteToggled = viewModel::onFavoriteToggled,
        onGroupClicked = viewModel::onGroupClicked,
        onQueryChanged = viewModel::onQueryChanged,
        onConversationOpened = {
            uiState.conversationToOpen?.let(onConversationStarted)
            viewModel.onConversationOpened()
        },
        onErrorShown = viewModel::onErrorShown,
        onFavoriteErrorShown = viewModel::onFavoriteErrorShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun ContactsScreen(
    uiState: ContactsUiState,
    onRefresh: () -> Unit,
    onContactClicked: (Contact) -> Unit,
    onFavoriteToggled: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
    onQueryChanged: (String) -> Unit,
    onConversationOpened: () -> Unit,
    onErrorShown: () -> Unit,
    onFavoriteErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }
    val favoriteErrorMessage = uiState.favoriteError?.let { stringResource(it.messageResource()) }
    LaunchedEffect(favoriteErrorMessage) {
        if (favoriteErrorMessage != null) {
            snackbarHostState.showSnackbar(favoriteErrorMessage)
            onFavoriteErrorShown()
        }
    }
    LaunchedEffect(uiState.conversationToOpen) {
        if (uiState.conversationToOpen != null) onConversationOpened()
    }

    TitledScreen(
        title = stringResource(R.string.new_chat_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
        searchBar = if (uiState.showSearch) {
            {
                SearchAppBar(
                    query = uiState.query,
                    onQueryChange = onQueryChanged,
                    onClose = onBack,
                    placeholder = stringResource(R.string.contacts_search_placeholder),
                    focusOnShow = uiState.isLoaded && uiState.favorites.isEmpty() && uiState.recent.isEmpty(),
                    closeDescription = stringResource(R.string.navigate_back),
                )
            }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isStartingConversation || uiState.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            ContactsContent(
                uiState = uiState,
                onRefresh = onRefresh,
                onContactClicked = onContactClicked,
                onFavoriteToggled = onFavoriteToggled,
                onGroupClicked = onGroupClicked,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    onRefresh: () -> Unit,
    onContactClicked: (Contact) -> Unit,
    onFavoriteToggled: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        ContactList(uiState, onContactClicked, onFavoriteToggled, onGroupClicked)
    }
}

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onContactClicked: (Contact) -> Unit,
    onFavoriteToggled: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        emptyStates(uiState)
        if (uiState.showRecent) {
            people(
                uiState,
                PeopleSection("recent", R.string.contacts_recent, uiState.recent),
                onContactClicked,
                onFavoriteToggled,
            )
        }
        if (uiState.showFavorites) {
            val favorites = PeopleSection("favorite", R.string.contacts_favorites, uiState.favorites)
            people(uiState, favorites, onContactClicked, onFavoriteToggled)
        }
        if (uiState.showPeopleHeader) {
            val found = PeopleSection("contact", R.string.contacts_people, uiState.contacts)
            people(uiState, found, onContactClicked, onFavoriteToggled)
        }
        if (uiState.groups.isNotEmpty()) groups(uiState, onGroupClicked)
    }
}

private fun LazyListScope.emptyStates(uiState: ContactsUiState) {
    if (uiState.showEmptyMessage) {
        item(key = "empty") {
            EmptyState(
                R.drawable.illustration_contacts,
                R.string.contacts_empty,
                R.string.contacts_empty_hint,
                Modifier.fillParentMaxSize(),
            )
        }
    }
    if (uiState.showSearchPrompt) {
        item(key = "search-prompt") {
            EmptyState(
                R.drawable.illustration_search,
                R.string.contacts_search_prompt,
                R.string.contacts_search_prompt_hint,
                Modifier.fillParentMaxSize(),
            )
        }
    }
    if (uiState.showNoResults) {
        item(key = "no-results") { NoResultsState(Modifier.fillParentMaxSize()) }
    }
}

/** The [key] says which section a row is in, so a person who is in two sections is two rows. */
private data class PeopleSection(val key: String, @StringRes val header: Int, val contacts: List<Contact>)

private fun LazyListScope.people(
    uiState: ContactsUiState,
    section: PeopleSection,
    onContactClicked: (Contact) -> Unit,
    onFavoriteToggled: (Contact) -> Unit,
) {
    item(key = "${section.key}-header") { SectionHeader(section.header) }
    items(section.contacts, key = { "${section.key}:${it.orgId}:${it.userId}" }) { contact ->
        ContactRow(contact, uiState, onContactClicked, onFavoriteToggled)
    }
}

private fun LazyListScope.groups(uiState: ContactsUiState, onGroupClicked: (Group) -> Unit) {
    item(key = "groups-header") { SectionHeader(R.string.contacts_groups) }
    items(uiState.groups, key = { "group:${it.id}" }) { group ->
        ListItem(
            modifier = Modifier.clickable(
                enabled = !uiState.isStartingConversation,
                onClickLabel = stringResource(R.string.conversation_open),
            ) { onGroupClicked(group) },
            leadingContent = { InitialAvatar(group.name) },
            headlineContent = { Text(group.name) },
        )
    }
}

@StringRes
private fun ContactsError.messageResource(): Int = when (this) {
    ContactsError.REFRESH_NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    ContactsError.REFRESH_UNKNOWN -> R.string.refresh_error_unknown
    ContactsError.START_NETWORK_UNAVAILABLE -> R.string.start_conversation_error_network_unavailable
    ContactsError.START_UNKNOWN -> R.string.start_conversation_error_unknown
}

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
