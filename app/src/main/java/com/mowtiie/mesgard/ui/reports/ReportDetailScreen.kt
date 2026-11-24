package com.mowtiie.mesgard.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.OverflowMenu
import com.mowtiie.mesgard.ui.common.TitledScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportDetailViewModel = hiltViewModel(),
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
    ReportDialogs(
        dialog = uiState.dialog,
        followUp = uiState.detail?.followUp(),
        onDecisionConfirmed = viewModel::onDecisionConfirmed,
        onRevealConfirmed = viewModel::onRevealConfirmed,
        onDismiss = viewModel::onDialogDismissed,
    )
    TitledScreen(
        title = stringResource(R.string.report_detail_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            if (uiState.showReveal) {
                OverflowMenu { dismiss ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_reveal)) },
                        onClick = {
                            dismiss()
                            viewModel.onDialogRequested(ReportDialog.Reveal)
                        },
                    )
                }
            }
        },
        onRefresh = if (uiState.isLoading) null else viewModel::onRefresh,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.detail == null,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                uiState.detail?.let { detail ->
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        reportDetailItems(
                            detail = detail,
                            actions = ReportActions(
                                enabled = !uiState.isWorking,
                                onStartReview = viewModel::onReviewStarted,
                                onDecide = { viewModel.onDialogRequested(ReportDialog.Decide(it)) },
                            ),
                        )
                    }
                }
            }
        }
    }
}
