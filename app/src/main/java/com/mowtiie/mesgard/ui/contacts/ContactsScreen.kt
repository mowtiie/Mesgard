package com.mowtiie.mesgard.ui.contacts

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.SearchAction
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen

@Composable
internal fun ContactsScreenRoute(
    onConversationStarted: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ContactsScreen(
        uiState = uiState,
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
        modifier = modifier,
    )
}

@Composable
internal fun ContactsScreen(
    uiState: ContactsUiState,
    onRefresh: () -> Unit,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
    onSectionSelected: (ContactsSection) -> Unit,
    onQueryChanged: (String) -> Unit,
    onConversationOpened: () -> Unit,
    onErrorShown: () -> Unit,
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
    LaunchedEffect(uiState.conversationToOpen) {
        if (uiState.conversationToOpen != null) onConversationOpened()
    }
    var isSearching by rememberSaveable { mutableStateOf(false) }

    TitledScreen(
        title = stringResource(R.string.tab_contacts),
        modifier = modifier,
        actions = {
            if (uiState.showSearch) SearchAction { isSearching = true }
            OverflowMenu { dismiss ->
                RefreshMenuItem(enabled = !uiState.isRefreshing) {
                    dismiss()
                    onRefresh()
                }
            }
        },
        searchBar = if (isSearching) {
            {
                SearchAppBar(
                    query = uiState.query,
                    onQueryChange = onQueryChanged,
                    onClose = {
                        isSearching = false
                        onQueryChanged("")
                    },
                    placeholder = stringResource(R.string.contacts_search_placeholder),
                )
            }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isStartingConversation) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (uiState.showSections) SectionTabs(uiState.section, onSectionSelected)
            ContactsContent(
                uiState = uiState,
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
            SectionPager(uiState, onContactClicked, onGroupClicked, onSectionSelected)
        } else {
            ContactList(ContactsSection.PEOPLE, uiState, onContactClicked, onGroupClicked)
        }
    }
}

@Composable
internal fun ContactList(
    section: ContactsSection,
    uiState: ContactsUiState,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (uiState.showEmptyMessage && section == ContactsSection.PEOPLE) {
            item(key = "empty") { CenteredMessage(R.string.contacts_empty) }
        }
        if (uiState.showNoResults(section)) {
            item(key = "no-results") { CenteredMessage(R.string.search_no_results) }
        }
        if (section == ContactsSection.PEOPLE) {
            items(uiState.contacts, key = { "contact:${it.orgId}:${it.userId}" }) { contact ->
                ListItem(
                    modifier = Modifier.clickable(enabled = !uiState.isStartingConversation) {
                        onContactClicked(contact)
                    },
                    leadingContent = { InitialAvatar(contact.displayName) },
                    headlineContent = { Text(contact.displayName) },
                    supportingContent = { Text(stringResource(contact.role.labelResource())) },
                    trailingContent = if (contact.userId in uiState.onlineUserIds) {
                        { OnlineLabel() }
                    } else {
                        null
                    },
                )
            }
        } else {
            items(uiState.groups, key = { "group:${it.id}" }) { group ->
                ListItem(
                    modifier = Modifier.clickable(enabled = !uiState.isStartingConversation) { onGroupClicked(group) },
                    leadingContent = { InitialAvatar(group.name) },
                    headlineContent = { Text(group.name) },
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.CenteredMessage(@StringRes message: Int) {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyLarge)
    }
}

@StringRes
private fun OrgRole.labelResource(): Int = when (this) {
    OrgRole.ORG_ADMIN -> R.string.role_org_admin
    OrgRole.STAFF -> R.string.role_staff
    OrgRole.STUDENT -> R.string.role_student
    OrgRole.OTHER -> R.string.role_member
}

@StringRes
private fun ContactsError.messageResource(): Int = when (this) {
    ContactsError.REFRESH_NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    ContactsError.REFRESH_UNKNOWN -> R.string.refresh_error_unknown
    ContactsError.START_NETWORK_UNAVAILABLE -> R.string.start_conversation_error_network_unavailable
    ContactsError.START_UNKNOWN -> R.string.start_conversation_error_unknown
}

@Composable
private fun OnlineLabel() {
    Text(
        text = stringResource(R.string.contact_online),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
