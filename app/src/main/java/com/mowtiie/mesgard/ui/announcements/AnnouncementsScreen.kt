package com.mowtiie.mesgard.ui.announcements

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.TitledScreen

private val FeedPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
private val CardSpacing = 12.dp

@Composable
internal fun AnnouncementsScreenRoute(
    viewModel: AnnouncementsViewModel,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisibilityChanged(true)
        onPauseOrDispose { viewModel.onVisibilityChanged(false) }
    }
    AnnouncementsScreen(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onRefreshErrorShown = viewModel::onRefreshErrorShown,
        onOpenConversation = onOpenConversation,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnouncementsScreen(
    uiState: AnnouncementsUiState,
    onRefresh: () -> Unit,
    onRefreshErrorShown: () -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.refreshError?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onRefreshErrorShown()
        }
    }
    val conversationId = uiState.conversationId
    TitledScreen(
        title = stringResource(R.string.announcements_title),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.canPost && conversationId != null) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenConversation(conversationId) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.announcements_new)) },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (uiState.showEmptyMessage) {
                EmptyState(
                    R.drawable.illustration_chats,
                    R.string.announcements_empty,
                    R.string.announcements_empty_hint,
                )
            }
            AnnouncementFeed(uiState, onOpenConversation)
        }
    }
}

@Composable
private fun AnnouncementFeed(uiState: AnnouncementsUiState, onOpenConversation: (String) -> Unit) {
    val nowEpochMillis = System.currentTimeMillis()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = FeedPadding,
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        items(uiState.announcements, key = { it.id }) { announcement ->
            AnnouncementCard(
                announcement = announcement,
                nowEpochMillis = nowEpochMillis,
                onClick = { uiState.conversationId?.let(onOpenConversation) },
            )
        }
    }
}

@StringRes
private fun RefreshError.messageResource(): Int = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    RefreshError.UNKNOWN -> R.string.refresh_error_unknown
}
