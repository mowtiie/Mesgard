package com.mowtiie.mesgard.ui.admin.audit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.TitledScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuditLogScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuditLogViewModel = hiltViewModel(),
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
        title = stringResource(R.string.audit_title),
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
                isRefreshing = uiState.isLoading && uiState.entries.isEmpty(),
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                AuditList(uiState, onLoadMore = viewModel::onLoadMore)
            }
        }
    }
}

@Composable
private fun AuditList(uiState: AuditLogUiState, onLoadMore: () -> Unit) {
    if (uiState.showEmptyMessage) {
        EmptyState(R.drawable.illustration_audit_log, R.string.audit_empty, R.string.audit_empty_hint)
    }
    val labels = rememberAuditLabels()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.entries, key = { it.id }) { entry -> AuditEntryItem(entry, uiState.names, labels) }
        if (uiState.hasMore) {
            item(key = "load-more") { LaunchedEffect(Unit) { onLoadMore() } }
        }
    }
}
