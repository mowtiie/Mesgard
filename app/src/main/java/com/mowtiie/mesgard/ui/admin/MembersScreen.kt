package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.SearchAction
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.labelResource

@Composable
internal fun MembersScreenRoute(
    onBack: () -> Unit,
    onAddMember: () -> Unit,
    onImportMembers: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MembersScreen(
        uiState = uiState,
        onBack = onBack,
        onAddMember = onAddMember,
        onImportMembers = onImportMembers,
        onRefresh = viewModel::onRefresh,
        onQueryChanged = viewModel::onQueryChanged,
        onMemberAction = viewModel::onMemberAction,
        onRoleSelected = viewModel::onRoleSelected,
        onDialogConfirmed = viewModel::onDialogConfirmed,
        onDialogDismissed = viewModel::onDialogDismissed,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MembersScreen(
    uiState: MembersUiState,
    onBack: () -> Unit,
    onAddMember: () -> Unit,
    onImportMembers: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onMemberAction: (Contact, MemberAction) -> Unit,
    onRoleSelected: (OrgRole) -> Unit,
    onDialogConfirmed: () -> Unit,
    onDialogDismissed: () -> Unit,
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
    var isSearching by rememberSaveable { mutableStateOf(false) }

    TitledScreen(
        title = stringResource(R.string.members_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            SearchAction { isSearching = true }
            OverflowMenu { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.members_import)) },
                    onClick = {
                        dismiss()
                        onImportMembers()
                    },
                )
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
                    placeholder = stringResource(R.string.members_search_placeholder),
                )
            }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMember) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.members_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                MemberList(uiState, onMemberAction)
            }
        }
    }
    MembersDialogs(uiState.dialog, uiState.isWorking, onRoleSelected, onDialogConfirmed, onDialogDismissed)
}

@Composable
private fun MemberList(uiState: MembersUiState, onMemberAction: (Contact, MemberAction) -> Unit) {
    if (uiState.showEmptyMessage || uiState.showNoResults) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(
                    if (uiState.showNoResults) R.string.search_no_results else R.string.members_empty,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        items(uiState.members, key = { "${it.orgId}:${it.userId}" }) { member ->
            ListItem(
                leadingContent = { InitialAvatar(member.displayName) },
                headlineContent = { Text(member.displayName) },
                supportingContent = { Text(stringResource(member.role.labelResource())) },
                trailingContent = { MemberMenu(member, onMemberAction) },
            )
        }
    }
}

@Composable
private fun MemberMenu(member: Contact, onMemberAction: (Contact, MemberAction) -> Unit) {
    OverflowMenu(description = stringResource(R.string.members_actions, member.displayName)) { dismiss ->
        MemberMenuItem(R.string.members_change_role) {
            dismiss()
            onMemberAction(member, MemberAction.CHANGE_ROLE)
        }
        MemberMenuItem(R.string.members_reset_password) {
            dismiss()
            onMemberAction(member, MemberAction.RESET_PASSWORD)
        }
        MemberMenuItem(R.string.members_remove) {
            dismiss()
            onMemberAction(member, MemberAction.REMOVE)
        }
    }
}

@Composable
private fun MemberMenuItem(@StringRes label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

@StringRes
private fun MembersError.messageResource(): Int = when (this) {
    MembersError.REFRESH_NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    MembersError.REFRESH_UNKNOWN -> R.string.refresh_error_unknown
    MembersError.NETWORK_UNAVAILABLE -> R.string.admin_error_network_unavailable
    MembersError.NOT_ALLOWED -> R.string.admin_error_not_allowed
    MembersError.NOT_A_MEMBER -> R.string.admin_error_not_a_member
    MembersError.LAST_ADMIN -> R.string.admin_error_last_admin
    MembersError.UNKNOWN -> R.string.admin_error_unknown
}
