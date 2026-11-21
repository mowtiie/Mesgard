package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSummary
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ChipSpacing = 8.dp
private val ChipPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportsScreenRoute(
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
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
        title = stringResource(R.string.reports_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        onRefresh = if (uiState.isLoading) null else viewModel::onRefresh,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StatusChips(uiState.status, viewModel::onStatusSelected)
            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.reports.isEmpty(),
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                ReportList(uiState, onOpenReport, onLoadMore = viewModel::onLoadMore)
            }
        }
    }
}

/** One status at a time, like the filters of Chats, so they are single choice filter chips. */
@Composable
private fun StatusChips(selected: ReportStatus, onSelected: (ReportStatus) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = ChipPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        ReportStatus.entries.forEach { status ->
            FilterChip(
                selected = status == selected,
                onClick = { onSelected(status) },
                label = { Text(stringResource(status.labelResource())) },
            )
        }
    }
}

@Composable
private fun ReportList(uiState: ReportsUiState, onOpenReport: (String) -> Unit, onLoadMore: () -> Unit) {
    if (uiState.showEmptyMessage) {
        EmptyState(R.drawable.illustration_audit_log, R.string.reports_empty, R.string.reports_empty_hint)
    }
    val formatTime = rememberDateTimeFormatter()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.reports, key = { it.id }) { report ->
            ReportRow(report, formatTime(report.createdAtEpochMillis), onClick = { onOpenReport(report.id) })
        }
        if (uiState.hasMore) {
            item(key = "load-more") { LaunchedEffect(Unit) { onLoadMore() } }
        }
    }
}

@Composable
private fun ReportRow(report: ReportSummary, time: String, onClick: () -> Unit) {
    val reporter = report.reporterName ?: stringResource(R.string.report_reporter_anonymous)
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        overlineContent = { Text(stringResource(report.category.labelResource())) },
        headlineContent = { Text(report.subjectName) },
        supportingContent = { Text(stringResource(R.string.report_row_summary, reporter, time)) },
    )
}
