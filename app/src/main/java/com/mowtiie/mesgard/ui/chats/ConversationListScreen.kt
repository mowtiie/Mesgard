package com.mowtiie.mesgard.ui.chats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.ui.common.TitledScreen

@Composable
internal fun ConversationListRoute(
    scrollToTopRequests: Int,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationListScreen(
        uiState = uiState,
        scrollToTopRequests = scrollToTopRequests,
        onRefresh = viewModel::onRefresh,
        onRefreshErrorShown = viewModel::onRefreshErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListScreen(
    uiState: ConversationListUiState,
    scrollToTopRequests: Int,
    onRefresh: () -> Unit,
    onRefreshErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val refreshErrorMessage = uiState.refreshError?.let { stringResource(it.messageResource()) }

    LaunchedEffect(refreshErrorMessage) {
        if (refreshErrorMessage != null) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
            onRefreshErrorShown()
        }
    }
    LaunchedEffect(scrollToTopRequests) {
        if (scrollToTopRequests > 0) listState.animateScrollToItem(0)
    }

    TitledScreen(
        title = stringResource(R.string.tab_chats),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            ConversationList(uiState = uiState, listState = listState)
        }
    }
}

@Composable
private fun ConversationList(uiState: ConversationListUiState, listState: LazyListState) {
    val nowEpochMillis = System.currentTimeMillis()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (uiState.showEmptyMessage) {
            item(key = "empty") {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.conversation_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        items(uiState.conversations, key = { it.id }) { conversation ->
            ConversationRow(conversation = conversation, nowEpochMillis = nowEpochMillis)
        }
    }
}

@StringRes
private fun RefreshError.messageResource(): Int = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    RefreshError.UNKNOWN -> R.string.refresh_error_unknown
}
