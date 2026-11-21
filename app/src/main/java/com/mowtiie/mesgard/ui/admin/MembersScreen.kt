package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NoResultsState
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.SearchAction
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.labelResource
import com.mowtiie.mesgard.ui.legalhold.PlaceHoldDialog

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
        onEraseToggled = viewModel::onEraseToggled,
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
    onEraseToggled: (Boolean) -> Unit,
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
    var holdTarget by remember { mutableStateOf<HoldTarget?>(null) }

    TitledScreen(
        title = stringResource(R.string.members_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            SearchAction { isSearching = true }
            IconButton(onClick = onImportMembers) {
                Icon(
                    painterResource(R.drawable.ic_upload),
                    contentDescription = stringResource(R.string.members_import),
                )
            }
        },
        onRefresh = if (uiState.isRefreshing) null else onRefresh,
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
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.members_add))
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
                MemberList(uiState, onMemberAction, onLegalHold = {
                    holdTarget =
                        HoldTarget.Person(it.userId, it.displayName)
                })
            }
        }
    }
    holdTarget?.let { PlaceHoldDialog(it, onDismiss = { holdTarget = null }, onPlaced = { holdTarget = null }) }
    MembersDialogs(
        dialog = uiState.dialog,
        isWorking = uiState.isWorking,
        erasureMode = uiState.erasureMode,
        onRoleSelected = onRoleSelected,
        onEraseToggled = onEraseToggled,
        onConfirm = onDialogConfirmed,
        onDismiss = onDialogDismissed,
    )
}

@Composable
private fun MemberList(
    uiState: MembersUiState,
    onMemberAction: (Contact, MemberAction) -> Unit,
    onLegalHold: (Contact) -> Unit,
) {
    if (uiState.showNoResults) {
        NoResultsState()
    } else if (uiState.showEmptyMessage) {
        EmptyState(R.drawable.illustration_members, R.string.members_empty, R.string.members_empty_hint)
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        items(uiState.members, key = { "${it.orgId}:${it.userId}" }) { member ->
            ListItem(
                leadingContent = { InitialAvatar(member.displayName) },
                headlineContent = { Text(member.displayName) },
                supportingContent = { Text(stringResource(member.role.labelResource())) },
                trailingContent = { MemberMenu(member, onMemberAction, onLegalHold) },
            )
        }
    }
}

@Composable
private fun MemberMenu(
    member: Contact,
    onMemberAction: (Contact, MemberAction) -> Unit,
    onLegalHold: (Contact) -> Unit,
) {
    OverflowMenu(description = stringResource(R.string.members_actions, member.displayName)) { dismiss ->
        MemberMenuItem(R.string.members_change_role) {
            dismiss()
            onMemberAction(member, MemberAction.CHANGE_ROLE)
        }
        MemberMenuItem(R.string.members_reset_password) {
            dismiss()
            onMemberAction(member, MemberAction.RESET_PASSWORD)
        }
        MemberMenuItem(R.string.members_reset_mfa) {
            dismiss()
            onMemberAction(member, MemberAction.RESET_MFA)
        }
        MemberMenuItem(R.string.legal_hold_menu) {
            dismiss()
            onLegalHold(member)
        }
        HorizontalDivider()
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
    MembersError.NOT_ERASABLE -> R.string.admin_error_not_erasable
    MembersError.UNKNOWN -> R.string.admin_error_unknown
}
