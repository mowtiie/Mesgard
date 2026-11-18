package com.mowtiie.mesgard.ui.admin.former

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.FormerMember
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.TitledScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormerMembersScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormerMembersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(R.string.former_members_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            OverflowMenu { dismiss ->
                RefreshMenuItem(enabled = !uiState.isLoading) {
                    dismiss()
                    viewModel.onRefresh()
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.members.isEmpty(),
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                FormerMembersList(uiState, viewModel::onEraseClicked, viewModel::onLoadMore)
            }
        }
    }
    uiState.confirming?.let { member ->
        EraseDialog(
            member,
            uiState.erasureMode,
            uiState.isWorking,
            viewModel::onEraseConfirmed,
            viewModel::onEraseDismissed,
        )
    }
}

@Composable
private fun FormerMembersList(uiState: FormerMembersUiState, onErase: (FormerMember) -> Unit, onLoadMore: () -> Unit) {
    if (uiState.showEmptyMessage) {
        EmptyState(R.drawable.illustration_members, R.string.former_members_empty, R.string.former_members_empty_hint)
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.members, key = { it.userId }) { member ->
            FormerMemberItem(member, showErase = uiState.canErase(member), onErase = { onErase(member) })
        }
        if (uiState.hasMore) {
            item(key = "load-more") { LaunchedEffect(Unit) { onLoadMore() } }
        }
    }
}

@Composable
private fun FormerMemberItem(member: FormerMember, showErase: Boolean, onErase: () -> Unit) {
    val context = LocalContext.current
    val date = DateUtils.formatDateTime(
        context,
        member.removedAtEpochMillis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
    )
    ListItem(
        leadingContent = { InitialAvatar(member.displayName) },
        headlineContent = { Text(member.displayName) },
        supportingContent = {
            Text(
                stringResource(
                    if (member.isErased) R.string.former_members_erased else R.string.former_members_removed,
                    date,
                ),
            )
        },
        trailingContent = {
            if (showErase) {
                TextButton(onClick = onErase) { Text(stringResource(R.string.former_members_erase)) }
            }
        },
    )
}

@Composable
private fun EraseDialog(
    member: FormerMember,
    mode: ErasureMode,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.former_members_erase_title, member.displayName)) },
        text = {
            Text(
                stringResource(
                    if (mode ==
                        ErasureMode.BLANK_MESSAGES
                    ) {
                        R.string.members_erase_help_blank
                    } else {
                        R.string.members_erase_help_keep
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                Text(stringResource(R.string.former_members_erase))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text(stringResource(R.string.cancel)) }
        },
    )
}
