package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NoResultsState
import com.mowtiie.mesgard.ui.common.SearchAction
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen

private val FabClearance = 88.dp
private val LevelIndent = 16.dp

@Composable
internal fun GroupsScreenRoute(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GroupsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenGroup = onOpenGroup,
        onRefresh = viewModel::onRefresh,
        onQueryChanged = viewModel::onQueryChanged,
        onCreateGroup = onCreateGroup,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupsScreen(
    uiState: GroupsUiState,
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onRefresh: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onCreateGroup: () -> Unit,
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

    TitledScreen(
        title = stringResource(R.string.groups_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = {
            SearchAction { isSearching = true }
        },
        onRefresh = if (uiState.isLoading) null else onRefresh,
        searchBar = if (isSearching) {
            {
                SearchAppBar(
                    query = uiState.query,
                    onQueryChange = onQueryChanged,
                    onClose = {
                        isSearching = false
                        onQueryChanged("")
                    },
                    placeholder = stringResource(R.string.groups_search_placeholder),
                )
            }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroup) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.groups_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                GroupList(uiState, onOpenGroup)
            }
        }
    }
}

@Composable
private fun GroupList(uiState: GroupsUiState, onOpenGroup: (String) -> Unit) {
    if (uiState.showNoResults) {
        NoResultsState()
    } else if (uiState.showEmptyMessage) {
        EmptyState(R.drawable.illustration_group_no_group, R.string.groups_empty, R.string.groups_empty_hint)
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FabClearance)) {
        items(uiState.rows, key = { it.group.id }) { row ->
            val group = row.group
            ListItem(
                modifier = Modifier.padding(start = LevelIndent * row.depth).clickable { onOpenGroup(group.id) },
                leadingContent = { InitialAvatar(group.name) },
                headlineContent = { Text(group.name) },
                supportingContent = {
                    val members =
                        pluralStringResource(R.plurals.groups_member_count, group.memberCount, group.memberCount)
                    Text(
                        stringResource(
                            R.string.groups_row_summary,
                            stringResource(group.kind.labelResource()),
                            members,
                        ),
                    )
                },
            )
        }
    }
}
