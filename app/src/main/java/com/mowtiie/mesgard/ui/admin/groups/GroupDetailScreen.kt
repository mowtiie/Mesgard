package com.mowtiie.mesgard.ui.admin.groups

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.TitledScreen

private val FabClearance = 88.dp

@Composable
internal fun GroupDetailScreenRoute(
    onBack: () -> Unit,
    onAddMembers: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onBack() }
    GroupDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onAddMembers = onAddMembers,
        onRefresh = viewModel::onRefresh,
        onDialogRequested = viewModel::onDialogRequested,
        onDialogDismissed = viewModel::onDialogDismissed,
        onRenameConfirmed = viewModel::onRenameConfirmed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onRemoveConfirmed = viewModel::onRemoveConfirmed,
        onRoleToggled = viewModel::onRoleToggled,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailScreen(
    uiState: GroupDetailUiState,
    onBack: () -> Unit,
    onAddMembers: () -> Unit,
    onRefresh: () -> Unit,
    onDialogRequested: (GroupDetailDialog) -> Unit,
    onDialogDismissed: () -> Unit,
    onRenameConfirmed: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onRemoveConfirmed: (GroupMemberRow) -> Unit,
    onRoleToggled: (GroupMemberRow) -> Unit,
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

    TitledScreen(
        title = uiState.name,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            OverflowMenu { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.group_rename)) },
                    onClick = {
                        dismiss()
                        onDialogRequested(GroupDetailDialog.Rename)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.group_delete)) },
                    onClick = {
                        dismiss()
                        onDialogRequested(GroupDetailDialog.ConfirmDelete)
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMembers) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.group_add_members))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                MemberList(uiState, onDialogRequested, onRoleToggled)
            }
        }
    }
    GroupDetailDialogs(uiState, onRenameConfirmed, onDeleteConfirmed, onRemoveConfirmed, onDialogDismissed)
}

@Composable
private fun MemberList(
    uiState: GroupDetailUiState,
    onDialogRequested: (GroupDetailDialog) -> Unit,
    onRoleToggled: (GroupMemberRow) -> Unit,
) {
    if (uiState.members.isEmpty() && !uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.group_members_empty), style = MaterialTheme.typography.bodyLarge)
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FabClearance)) {
        items(uiState.members, key = { it.userId }) { member ->
            val name = member.displayName ?: stringResource(R.string.group_unknown_member)
            ListItem(
                leadingContent = { InitialAvatar(name) },
                headlineContent = { Text(name) },
                supportingContent = { Text(stringResource(member.role.labelResource())) },
                trailingContent = {
                    OverflowMenu(description = stringResource(R.string.members_actions, name)) { dismiss ->
                        DropdownMenuItem(
                            text = { Text(stringResource(member.role.toggleResource())) },
                            onClick = {
                                dismiss()
                                onRoleToggled(member)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.group_remove_member)) },
                            onClick = {
                                dismiss()
                                onDialogRequested(GroupDetailDialog.ConfirmRemove(member))
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun GroupDetailDialogs(
    uiState: GroupDetailUiState,
    onRenameConfirmed: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onRemoveConfirmed: (GroupMemberRow) -> Unit,
    onDismiss: () -> Unit,
) {
    when (val dialog = uiState.dialog) {
        GroupDetailDialog.Rename -> GroupNameDialog(
            title = R.string.group_rename_title,
            confirmLabel = R.string.group_rename_confirm,
            initialName = uiState.name,
            isWorking = uiState.isWorking,
            onConfirm = onRenameConfirmed,
            onDismiss = onDismiss,
        )
        GroupDetailDialog.ConfirmDelete -> GroupConfirmDialog(
            title = R.string.group_delete_title,
            message = stringResource(R.string.group_delete_message, uiState.name),
            confirmLabel = R.string.group_delete_confirm,
            isWorking = uiState.isWorking,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDismiss,
        )
        is GroupDetailDialog.ConfirmRemove -> GroupConfirmDialog(
            title = R.string.group_remove_title,
            message = stringResource(
                R.string.group_remove_message,
                dialog.member.displayName ?: stringResource(R.string.group_unknown_member),
                uiState.name,
            ),
            confirmLabel = R.string.group_remove_confirm,
            isWorking = uiState.isWorking,
            onConfirm = { onRemoveConfirmed(dialog.member) },
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}

private fun GroupRole.labelResource() =
    if (this == GroupRole.MODERATOR) R.string.group_role_moderator else R.string.group_role_member

private fun GroupRole.toggleResource() =
    if (this == GroupRole.MODERATOR) R.string.group_make_member else R.string.group_make_moderator
