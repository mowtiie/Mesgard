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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.legalhold.PlaceHoldSheet

private val FabClearance = 88.dp

@Composable
internal fun GroupDetailScreenRoute(
    groupId: String,
    onBack: () -> Unit,
    onAddMembers: () -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    treeViewModel: GroupTreeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tree by treeViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onBack() }
    LaunchedEffect(uiState.hasLeft) { if (uiState.hasLeft) onLeft() }
    var isHoldDialogShown by remember { mutableStateOf(false) }
    if (isHoldDialogShown) {
        PlaceHoldSheet(
            target = HoldTarget.Group(groupId, uiState.name),
            onDismiss = { isHoldDialogShown = false },
            onPlaced = { isHoldDialogShown = false },
        )
    }
    GroupTreeDialogs(tree, treeViewModel)
    GroupDetailScreen(
        uiState = uiState,
        treeError = tree.error,
        onTreeErrorShown = treeViewModel::onErrorShown,
        placeSummary = placeSummary(tree),
        onChangeKind = { treeViewModel.onDialogChanged(GroupTreeDialog.KIND) },
        onMove = { treeViewModel.onDialogChanged(GroupTreeDialog.PARENT) },
        canAnnounce = tree.hasGroupsInside &&
            (uiState.isAdmin || uiState.ownRole == GroupRole.OWNER || uiState.ownRole == GroupRole.MODERATOR),
        onAnnounce = { treeViewModel.onDialogChanged(GroupTreeDialog.ANNOUNCE) },
        announcedTo = tree.announcedTo,
        onAnnouncementShown = treeViewModel::onAnnouncementShown,
        onLegalHold = { isHoldDialogShown = true },
        onBack = onBack,
        onAddMembers = onAddMembers,
        onRefresh = viewModel::onRefresh,
        onDialogRequested = viewModel::onDialogChanged,
        onDialogDismissed = { viewModel.onDialogChanged(null) },
        onRenameConfirmed = viewModel::onRenameConfirmed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onLeaveRequested = {
            viewModel.onDialogChanged(
                if (uiState.isOnlyOwner) GroupDetailDialog.OnlyOwner else GroupDetailDialog.ConfirmLeave,
            )
        },
        onLeaveConfirmed = viewModel::onLeaveConfirmed,
        onRemoveConfirmed = viewModel::onRemoveConfirmed,
        onRoleSelected = viewModel::onRoleSelected,
        onSettingsChanged = viewModel::onSettingsChanged,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailScreen(
    uiState: GroupDetailUiState,
    treeError: AdminError?,
    onTreeErrorShown: () -> Unit,
    placeSummary: String?,
    onChangeKind: () -> Unit,
    onMove: () -> Unit,
    canAnnounce: Boolean,
    onAnnounce: () -> Unit,
    announcedTo: Int?,
    onAnnouncementShown: () -> Unit,
    onLegalHold: () -> Unit,
    onBack: () -> Unit,
    onAddMembers: () -> Unit,
    onRefresh: () -> Unit,
    onDialogRequested: (GroupDetailDialog) -> Unit,
    onDialogDismissed: () -> Unit,
    onRenameConfirmed: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onLeaveRequested: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onRemoveConfirmed: (GroupMemberRow) -> Unit,
    onRoleSelected: (GroupMemberRow, GroupRole) -> Unit,
    onSettingsChanged: (GroupSettings) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = (uiState.error ?: treeError)?.let { stringResource(it.messageResource()) }
    val announcedMessage = announcedTo?.let { pluralStringResource(R.plurals.group_announced, it, it) }
    LaunchedEffect(announcedMessage) {
        if (announcedMessage != null) {
            snackbarHostState.showSnackbar(announcedMessage)
            onAnnouncementShown()
        }
    }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
            onTreeErrorShown()
        }
    }

    TitledScreen(
        title = uiState.name,
        subtitle = placeSummary,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            if (uiState.canManage) {
                IconButton(onClick = { onDialogRequested(GroupDetailDialog.Rename) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.group_rename_title))
                }
            }
            if (uiState.isAdmin) {
                IconButton(onClick = { onDialogRequested(GroupDetailDialog.ConfirmDelete) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.group_delete))
                }
            }
            GroupMenu(
                isAdmin = uiState.isAdmin,
                isMember = uiState.ownRole != null,
                canAnnounce = canAnnounce,
                actions = GroupMenuActions(onAnnounce, onChangeKind, onMove, onLegalHold, onLeaveRequested),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.canManage) {
                FloatingActionButton(onClick = onAddMembers) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.group_add_members))
                }
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
                MemberList(uiState, onDialogRequested, onRoleSelected, onSettingsChanged)
            }
        }
    }
    GroupDetailDialogs(
        uiState,
        GroupDetailActions(onRenameConfirmed, onDeleteConfirmed, onLeaveConfirmed, onRemoveConfirmed),
        onDialogDismissed,
    )
}

@Composable
private fun MemberList(
    uiState: GroupDetailUiState,
    onDialogRequested: (GroupDetailDialog) -> Unit,
    onRoleSelected: (GroupMemberRow, GroupRole) -> Unit,
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
        if (uiState.isAdmin) {
            item(key = "settings") {
                GroupSettingsRows(
                    uiState.settings,
                    enabled = !uiState.isWorking && !uiState.isLoading,
                    onChanged = onSettingsChanged,
                )
            }
        }
        items(uiState.members, key = { it.userId }) { member ->
            val name = member.displayName ?: stringResource(R.string.group_unknown_member)
            ListItem(
                leadingContent = { InitialAvatar(name) },
                headlineContent = { Text(name) },
                supportingContent = { Text(stringResource(member.role.labelResource())) },
                trailingContent = if (!uiState.canManage) {
                    null
                } else {
                    (
                        {
                            OverflowMenu(description = stringResource(R.string.members_actions, name)) { dismiss ->
                                GroupRole.entries.filter { it != member.role }.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(role.makeResource())) },
                                        onClick = {
                                            dismiss()
                                            onRoleSelected(member, role)
                                        },
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.group_remove_member)) },
                                    onClick = {
                                        dismiss()
                                        onDialogRequested(GroupDetailDialog.ConfirmRemove(member))
                                    },
                                )
                            }
                        }
                        )
                },
            )
        }
    }
}

@Composable
private fun GroupDetailDialogs(uiState: GroupDetailUiState, actions: GroupDetailActions, onDismiss: () -> Unit) {
    when (val dialog = uiState.dialog) {
        GroupDetailDialog.Rename -> GroupNameSheet(
            title = R.string.group_rename_title,
            confirmLabel = R.string.group_rename_confirm,
            initialName = uiState.name,
            isWorking = uiState.isWorking,
            onConfirm = actions.onRenameConfirmed,
            onDismiss = onDismiss,
        )
        GroupDetailDialog.ConfirmDelete -> GroupConfirmDialog(
            title = R.string.group_delete_title,
            message = stringResource(R.string.group_delete_message, uiState.name),
            confirmLabel = R.string.group_delete_confirm,
            isWorking = uiState.isWorking,
            onConfirm = actions.onDeleteConfirmed,
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
            onConfirm = { actions.onRemoveConfirmed(dialog.member) },
            onDismiss = onDismiss,
        )
        GroupDetailDialog.ConfirmLeave -> GroupConfirmDialog(
            title = R.string.group_leave_title,
            message = stringResource(R.string.group_leave_message, uiState.name),
            confirmLabel = R.string.group_leave_confirm,
            isWorking = uiState.isWorking,
            onConfirm = actions.onLeaveConfirmed,
            onDismiss = onDismiss,
        )
        GroupDetailDialog.OnlyOwner -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.group_only_owner_title)) },
            text = { Text(stringResource(R.string.group_only_owner_message)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.report_done)) } },
        )
        null -> Unit
    }
}

/** What the dialogs of the group screen do once confirmed. */
private class GroupDetailActions(
    val onRenameConfirmed: (String) -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onLeaveConfirmed: () -> Unit,
    val onRemoveConfirmed: (GroupMemberRow) -> Unit,
)

private fun GroupRole.labelResource() = when (this) {
    GroupRole.OWNER -> R.string.group_role_owner
    GroupRole.MODERATOR -> R.string.group_role_moderator
    GroupRole.MEMBER -> R.string.group_role_member
}

private fun GroupRole.makeResource() = when (this) {
    GroupRole.OWNER -> R.string.group_make_owner
    GroupRole.MODERATOR -> R.string.group_make_moderator
    GroupRole.MEMBER -> R.string.group_make_member
}
