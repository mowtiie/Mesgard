package com.mowtiie.mesgard.ui.admin.audit

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.RefreshMenuItem
import com.mowtiie.mesgard.ui.common.TitledScreen

private const val DATE_AND_TIME =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH

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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.audit_empty), style = MaterialTheme.typography.bodyLarge)
        }
    }
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    val formerMember = stringResource(R.string.audit_former_member)
    val roleLabels = mapOf(
        "org_admin" to stringResource(R.string.role_org_admin),
        "staff" to stringResource(R.string.role_staff),
        "student" to stringResource(R.string.role_student),
        "moderator" to stringResource(R.string.group_role_moderator),
        "member" to stringResource(R.string.group_role_member),
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.entries, key = { it.id }) { entry ->
            val message = describeAudit(entry, uiState.names, roleLabels, formerMember)
            ListItem(
                headlineContent = { Text(message.format(resources)) },
                supportingContent = {
                    Text(DateUtils.formatDateTime(context, entry.createdAtEpochMillis, DATE_AND_TIME))
                },
            )
        }
        if (uiState.hasMore) {
            item(key = "load-more") { LaunchedEffect(Unit) { onLoadMore() } }
        }
    }
}
