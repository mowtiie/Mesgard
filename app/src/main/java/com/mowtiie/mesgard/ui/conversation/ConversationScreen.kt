package com.mowtiie.mesgard.ui.conversation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.fallbackTitleResource
import java.time.ZoneId

private const val LOAD_OLDER_THRESHOLD = 5

@Composable
internal fun ConversationScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = hiltViewModel(),
    downloadsViewModel: AttachmentDownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloads by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisibilityChanged(true)
        onPauseOrDispose { viewModel.onVisibilityChanged(false) }
    }
    ConversationScreen(
        uiState = uiState,
        downloads = downloads,
        onBack = onBack,
        onDraftChanged = viewModel::onDraftChanged,
        onSendClicked = viewModel::onSendClicked,
        onRetryClicked = viewModel::onRetryClicked,
        onAttachmentsPicked = viewModel::onAttachmentsPicked,
        onAttachmentRemoved = viewModel::onAttachmentRemoved,
        onAttachmentClicked = downloadsViewModel::onAttachmentClicked,
        onImageShown = downloadsViewModel::onImageShown,
        onAttachmentOpened = downloadsViewModel::onAttachmentOpened,
        onDownloadErrorShown = downloadsViewModel::onErrorShown,
        onLoadOlder = viewModel::onLoadOlder,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@Composable
internal fun ConversationScreen(
    uiState: ConversationUiState,
    downloads: AttachmentDownloadsUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onRetryClicked: (String) -> Unit,
    onAttachmentsPicked: (List<PickedAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onImageShown: (Attachment) -> Unit,
    onAttachmentOpened: () -> Unit,
    onDownloadErrorShown: () -> Unit,
    onLoadOlder: () -> Unit,
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
    val downloadErrorMessage = downloads.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(downloadErrorMessage) {
        if (downloadErrorMessage != null) {
            snackbarHostState.showSnackbar(downloadErrorMessage)
            onDownloadErrorShown()
        }
    }
    val context = LocalContext.current
    val openFailedMessage = stringResource(R.string.conversation_error_open_attachment)
    LaunchedEffect(downloads.attachmentToOpen) {
        val attachment = downloads.attachmentToOpen ?: return@LaunchedEffect
        val opened = openAttachment(context, attachment)
        onAttachmentOpened()
        if (!opened) snackbarHostState.showSnackbar(openFailedMessage)
    }

    val title = uiState.title ?: uiState.type?.let { stringResource(it.fallbackTitleResource()) }.orEmpty()
    TitledScreen(
        title = title,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize()) {
            MessageList(
                uiState = uiState,
                downloadingIds = downloads.downloadingIds,
                onRetryClicked = onRetryClicked,
                onAttachmentClick = onAttachmentClicked,
                onImageShown = onImageShown,
                onLoadOlder = onLoadOlder,
                modifier = Modifier.weight(1f),
            )
            if (uiState.canPost && uiState.typingNames.isNotEmpty()) TypingIndicator(uiState.typingNames)
            if (uiState.canPost) {
                MessageComposer(
                    draft = uiState.draft,
                    attachments = uiState.attachments,
                    canSend = uiState.canSend,
                    onDraftChanged = onDraftChanged,
                    onSendClicked = onSendClicked,
                    onAttachmentsPicked = onAttachmentsPicked,
                    onAttachmentRemoved = onAttachmentRemoved,
                )
            } else {
                ReadOnlyNotice()
            }
        }
    }
}

@Composable
private fun MessageList(
    uiState: ConversationUiState,
    downloadingIds: Set<String>,
    onRetryClicked: (String) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onImageShown: (Attachment) -> Unit,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val timeline = remember(uiState.messages) { uiState.messages.withDateHeaders(ZoneId.systemDefault()) }
    val isNearOldestLoaded by remember { derivedStateOf { listState.isNearEnd() } }
    val newestMessage = uiState.messages.firstOrNull()

    LaunchedEffect(isNearOldestLoaded, uiState.messages.size) {
        if (isNearOldestLoaded && uiState.messages.isNotEmpty()) onLoadOlder()
    }
    LaunchedEffect(newestMessage?.id) {
        if (newestMessage?.isOwn == true) listState.animateScrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize()) {
            items(timeline, key = { it.key }) { item ->
                when (item) {
                    is TimelineItem.Bubble -> MessageBubble(
                        message = item.message,
                        showSenderName = uiState.showSenderNames,
                        onRetry = { onRetryClicked(item.message.id) },
                        downloadingIds = downloadingIds,
                        onAttachmentClick = onAttachmentClick,
                        onImageShown = onImageShown,
                    )
                    is TimelineItem.DateHeader -> DateHeader(item.date)
                }
            }
            if (uiState.isLoadingOlder) {
                item(key = "loading-older") { LoadingOlderIndicator() }
            }
        }
        if (uiState.showEmptyMessage) {
            Text(
                text = stringResource(R.string.conversation_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun LoadingOlderIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun LazyListState.isNearEnd(): Boolean {
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    return lastVisible.index >= layoutInfo.totalItemsCount - LOAD_OLDER_THRESHOLD
}

@StringRes
private fun ConversationError.messageResource(): Int = when (this) {
    ConversationError.NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    ConversationError.UNKNOWN -> R.string.refresh_error_unknown
    ConversationError.MESSAGE_TOO_LONG -> R.string.conversation_error_message_too_long
    ConversationError.NOT_SIGNED_IN -> R.string.conversation_error_not_signed_in
    ConversationError.CONVERSATION_UNAVAILABLE -> R.string.conversation_error_unavailable
    ConversationError.TOO_MANY_ATTACHMENTS -> R.string.conversation_error_too_many_attachments
    ConversationError.ATTACHMENT_TOO_LARGE -> R.string.conversation_error_attachment_too_large
    ConversationError.ATTACHMENT_TYPE_NOT_ALLOWED -> R.string.conversation_error_attachment_type
    ConversationError.ATTACHMENT_UNREADABLE -> R.string.conversation_error_attachment_unreadable
}
