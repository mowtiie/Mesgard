package com.mowtiie.mesgard.ui.admin.groups

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
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
        onSettingsChanged = viewModel::onSettingsChanged,
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
    onSettingsChanged: (GroupSettings) -> Unit,
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            IconButton(onClick = { onDialogRequested(GroupDetailDialog.Rename) }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.group_rename_title))
            }
            IconButton(onClick = { onDialogRequested(GroupDetailDialog.ConfirmDelete) }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.group_delete))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMembers) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.group_add_members))
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
                MemberList(uiState, onDialogRequested, onRoleToggled, onSettingsChanged)
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
    onSettingsChanged: (GroupSettings) -> Unit,
) {
    if (uiState.members.isEmpty() && !uiState.isLoading) {
        EmptyState(
            R.drawable.illustration_group_no_member,
            R.string.group_members_empty,
            R.string.group_members_empty_hint,
        )
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FabClearance)) {
        item(key = "settings") {
            GroupSettingsRows(
                uiState.settings,
                enabled = !uiState.isWorking && !uiState.isLoading,
                onChanged = onSettingsChanged,
            )
        }
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
                        HorizontalDivider()
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
