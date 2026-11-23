package com.mowtiie.mesgard.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.RuleSummaries
import com.mowtiie.mesgard.ui.common.ReadableWidth
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.server.ServerStatusNotice

@Composable
internal fun AdministrationScreenRoute(
    onBack: () -> Unit,
    navigation: AdminNavigation,
    modifier: Modifier = Modifier,
    viewModel: AdministrationViewModel = hiltViewModel(),
    summaryViewModel: AdministrationSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by summaryViewModel.summaries.collectAsStateWithLifecycle()
    var isFirstResume by remember { mutableStateOf(true) }
    // Something may have been changed on the page that was just closed, so read it again each time this one is shown.
    LifecycleResumeEffect(Unit) {
        summaryViewModel.refresh()
        if (!isFirstResume) viewModel.onRefresh()
        isFirstResume = false
        onPauseOrDispose {}
    }
    var wasAdmin by remember { mutableStateOf(false) }
    // A person who loses the role while this page is open goes back. The server refuses their requests anyway.
    LaunchedEffect(uiState.isAdmin) {
        if (uiState.isAdmin) {
            wasAdmin = true
        } else if (wasAdmin) {
            onBack()
        }
    }
    AdministrationScreen(
        uiState = uiState,
        summaries = summaries,
        navigation = navigation,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

private val FabClearance = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdministrationScreen(
    uiState: AdministrationUiState,
    summaries: RuleSummaries,
    navigation: AdminNavigation,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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
        title = stringResource(R.string.settings_administration),
        subtitle = uiState.organizationName,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            MemberFabMenu(onAddMember = navigation.onAddMember, onImportMembers = navigation.onImportMembers)
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = ReadableWidth).fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ServerStatusNotice(forAdministrator = true, modifier = Modifier.padding(16.dp))
                ManageSections(uiState, summaries, navigation)
                RecentActivitySection(uiState)
                Spacer(Modifier.height(FabClearance))
            }
        }
    }
}
