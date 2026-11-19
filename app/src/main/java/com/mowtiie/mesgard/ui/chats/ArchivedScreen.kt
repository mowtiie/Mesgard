package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.TitledScreen

@Composable
internal fun ArchivedScreenRoute(
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedViewModel = hiltViewModel(),
    selectionViewModel: ConversationSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selection by selectionViewModel.uiState.collectAsStateWithLifecycle()
    val callbacks = SelectionCallbacks(
        onToggled = selectionViewModel::onToggled,
        onSelectAll = { selectionViewModel.onSelectAll(uiState.conversations.map { it.id }) },
        onClose = selectionViewModel::onSelectionClosed,
        onErrorShown = selectionViewModel::onErrorShown,
        onPin = selectionViewModel::onPinClicked,
        onArchive = selectionViewModel::onArchiveClicked,
        onUndoArchiveHandled = selectionViewModel::onUndoArchiveHandled,
        onMute = selectionViewModel::onMuteClicked,
        onRead = selectionViewModel::onReadClicked,
    )
    ArchivedScreen(uiState, selection, callbacks, onBack, onConversationClick, modifier)
}

@Composable
internal fun ArchivedScreen(
    uiState: ArchivedUiState,
    selection: ConversationSelectionUiState,
    callbacks: SelectionCallbacks,
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = selection.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            callbacks.onErrorShown()
        }
    }
    TitledScreen(
        title = stringResource(R.string.archived_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        searchBar = if (selection.isActive) {
            { SelectionAppBar(state = selection, callbacks = callbacks) }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoaded && uiState.conversations.isEmpty()) {
            EmptyState(
                R.drawable.illustration_chats,
                R.string.archived_empty,
                R.string.archived_empty_hint,
                Modifier.padding(padding),
            )
        }
        val nowEpochMillis = System.currentTimeMillis()
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(uiState.conversations, key = { it.id }) { conversation ->
                SelectableConversationRow(conversation, nowEpochMillis, selection, callbacks, onConversationClick)
            }
        }
    }
}
