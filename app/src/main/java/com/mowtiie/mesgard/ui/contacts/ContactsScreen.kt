package com.mowtiie.mesgard.ui.contacts

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.SelectedAvatar
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.labelResource

@Composable
internal fun ContactsScreenRoute(
    onConversationStarted: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: ContactsViewModel = hiltViewModel(),
    selectionViewModel: ContactsSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.uiState.collectAsStateWithLifecycle()
    val selection = ContactsSelection(
        state = selectionState,
        callbacks = ContactsSelectionCallbacks(
            onToggled = selectionViewModel::onToggled,
            onSelectAll = {
                selectionViewModel.onSelectAll(if (uiState.showFavorites) uiState.favorites else uiState.contacts)
            },
            onClose = selectionViewModel::onSelectionClosed,
            onMessage = {
                viewModel.onContactClicked(it)
                selectionViewModel.onSelectionClosed()
            },
            onFavorite = selectionViewModel::onFavoriteClicked,
            onErrorShown = selectionViewModel::onErrorShown,
        ),
    )
    ContactsScreen(
        uiState = uiState,
        selection = selection,
        onRefresh = viewModel::onRefresh,
        onContactClicked = viewModel::onContactClicked,
        onGroupClicked = viewModel::onGroupClicked,
        onSectionSelected = viewModel::onSectionSelected,
        onQueryChanged = viewModel::onQueryChanged,
        onConversationOpened = {
            uiState.conversationToOpen?.let(onConversationStarted)
            viewModel.onConversationOpened()
        },
        onErrorShown = viewModel::onErrorShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun ContactsScreen(
    uiState: ContactsUiState,
    selection: ContactsSelection,
    onRefresh: () -> Unit,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
    onSectionSelected: (ContactsSection) -> Unit,
    onQueryChanged: (String) -> Unit,
    onConversationOpened: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }
    val selectionErrorMessage = selection.state.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(selectionErrorMessage) {
        if (selectionErrorMessage != null) {
            snackbarHostState.showSnackbar(selectionErrorMessage)
            selection.callbacks.onErrorShown()
        }
    }
    LaunchedEffect(uiState.conversationToOpen) {
        if (uiState.conversationToOpen != null) onConversationOpened()
    }

    TitledScreen(
        title = stringResource(if (onBack != null) R.string.new_chat_title else R.string.tab_contacts),
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                    )
                }
            }
        },
        actions = {
            OverflowMenu { dismiss ->
                RefreshMenuItem(enabled = !uiState.isRefreshing) {
                    dismiss()
                    onRefresh()
                }
            }
        },
        searchBar = if (selection.state.isActive) {
            { ContactsSelectionBar(selection.state, selection.callbacks) }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isStartingConversation || uiState.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (uiState.showSearch) ContactsSearchField(uiState.query, onQueryChanged)
            if (uiState.showSections) SectionTabs(uiState.section, onSectionSelected)
            ContactsContent(
                uiState = uiState,
                selection = selection,
                onRefresh = onRefresh,
                onContactClicked = onContactClicked,
                onGroupClicked = onGroupClicked,
                onSectionSelected = onSectionSelected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    selection: ContactsSelection,
    onRefresh: () -> Unit,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
    onSectionSelected: (ContactsSection) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (uiState.showSections) {
            SectionPager(uiState, selection, onContactClicked, onGroupClicked, onSectionSelected)
        } else {
            ContactList(ContactsSection.PEOPLE, uiState, selection, onContactClicked, onGroupClicked)
        }
    }
}

@Composable
internal fun ContactList(
    section: ContactsSection,
    uiState: ContactsUiState,
    selection: ContactsSelection,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (uiState.showEmptyMessage && section == ContactsSection.PEOPLE) {
            item(key = "empty") {
                EmptyState(
                    R.drawable.illustration_contacts,
                    R.string.contacts_empty,
                    R.string.contacts_empty_hint,
                    Modifier.fillParentMaxSize(),
                )
            }
        }
        if (uiState.showSearchPrompt && section == ContactsSection.PEOPLE) {
            item(key = "search-prompt") {
                EmptyState(
                    R.drawable.illustration_search,
                    R.string.contacts_search_prompt,
                    R.string.contacts_search_prompt_hint,
                    Modifier.fillParentMaxSize(),
                )
            }
        }
        if (uiState.showNoResults(section)) {
            item(key = "no-results") { NoResultsState(Modifier.fillParentMaxSize()) }
        }
        if (section == ContactsSection.PEOPLE) {
            if (uiState.showFavorites) {
                item(key = "favorites-header") { FavoritesHeader() }
                items(uiState.favorites, key = { "favorite:${it.orgId}:${it.userId}" }) { contact ->
                    ContactRow(contact, uiState, selection, onContactClicked)
                }
            }
            items(uiState.contacts, key = { "contact:${it.orgId}:${it.userId}" }) { contact ->
                ContactRow(contact, uiState, selection, onContactClicked)
            }
        } else {
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
private fun FavoritesHeader() {
    Text(
        text = stringResource(R.string.contacts_favorites),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ContactRow(
    contact: Contact,
    uiState: ContactsUiState,
    selection: ContactsSelection,
    onContactClicked: (Contact) -> Unit,
) {
    val isSelected = selection.state.isSelected(contact)
    ListItem(
        modifier = Modifier
            .combinedClickable(
                enabled = !uiState.isStartingConversation,
                onClickLabel = stringResource(R.string.conversation_start),
                onLongClickLabel = stringResource(R.string.selection_select),
                onLongClick = { selection.callbacks.onToggled(contact) },
            ) {
                if (selection.state.isActive) selection.callbacks.onToggled(contact) else onContactClicked(contact)
            }
            .semantics { selected = isSelected },
        colors = if (isSelected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        leadingContent = { if (isSelected) SelectedAvatar() else InitialAvatar(contact.displayName) },
        headlineContent = { Text(contact.displayName) },
        supportingContent = { Text(stringResource(contact.role.labelResource())) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contact.userId in uiState.onlineUserIds) OnlineLabel()
                if (contact.isFavorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.contact_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

@Composable
private fun OnlineLabel() {
    Text(
        text = stringResource(R.string.contact_online),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
