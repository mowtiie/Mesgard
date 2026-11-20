package com.mowtiie.mesgard.ui.conversation

import android.content.ClipData
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.fallbackTitleResource
import java.time.ZoneId
import kotlinx.coroutines.launch

private const val LOAD_OLDER_THRESHOLD = 5

@Composable
internal fun ConversationScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = hiltViewModel(),
    downloadsViewModel: AttachmentDownloadsViewModel = hiltViewModel(),
    failedMessagesViewModel: FailedMessagesViewModel = hiltViewModel(),
    actionsViewModel: ConversationActionsViewModel = hiltViewModel(),
    editViewModel: MessageEditViewModel = hiltViewModel(),
    replyViewModel: ReplyViewModel = hiltViewModel(),
    mentionViewModel: MentionViewModel = hiltViewModel(),
    searchViewModel: ConversationSearchViewModel = hiltViewModel(),
    pinsViewModel: PinsViewModel = hiltViewModel(),
    selectionViewModel: MessageSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloads by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisibilityChanged(true)
        actionsViewModel.onOpened()
        pinsViewModel.onRefresh()
        onPauseOrDispose { viewModel.onVisibilityChanged(false) }
    }
    val messageActions by actionsViewModel.uiState.collectAsStateWithLifecycle()
    if (messageActions.messagesToDelete.isNotEmpty()) {
        DeleteMessageDialog(
            count = messageActions.messagesToDelete.size,
            onConfirm = actionsViewModel::onDeleteConfirmed,
            onDismiss = actionsViewModel::onDeleteDismissed,
        )
    }
    messageActions.readers?.let { MessageInfoSheet(it, onDismiss = actionsViewModel::onInfoDismissed) }
    val edit by editViewModel.uiState.collectAsStateWithLifecycle()
    val reply by replyViewModel.target.collectAsStateWithLifecycle()
    val mentions by mentionViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val pins by pinsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedIds by selectionViewModel.selected.collectAsStateWithLifecycle()
    // Remembered, so typing in the composer does not rebuild every message bubble.
    val selectionCallbacks = remember {
        MessageSelectionCallbacks(
            onToggle = selectionViewModel::onToggled,
            onClear = selectionViewModel::onSelectionClosed,
            onReply = replyViewModel::onReplyRequested,
            onDelete = actionsViewModel::onDeleteRequested,
            onEdit = editViewModel::onEditRequested,
            onInfo = actionsViewModel::onInfoRequested,
            onPin = pinsViewModel::onPin,
            onUnpin = pinsViewModel::onUnpin,
        )
    }
    val bubbleActions = remember {
        MessageActions(
            onRetry = failedMessagesViewModel::onRetryClicked,
            onDiscard = failedMessagesViewModel::onDiscardClicked,
            onReply = replyViewModel::onReplyRequested,
            onAttachmentClick = downloadsViewModel::onAttachmentClicked,
            onImageShown = downloadsViewModel::onImageShown,
            onRetryDownload = downloadsViewModel::onRetryDownload,
        )
    }
    ConversationScreen(
        uiState = uiState.copy(error = uiState.error ?: messageActions.error ?: edit.error ?: pins.error),
        pins = pins.pins,
        edit = edit,
        reply = reply,
        onReplyDropped = replyViewModel::onReplyDropped,
        editActions = EditActions(
            onTextChanged = editViewModel::onTextChanged,
            onCancel = editViewModel::onEditCancelled,
            onSave = editViewModel::onSaveClicked,
        ),
        downloads = downloads,
        onBack = onBack,
        onDraftChanged = {
            viewModel.onDraftChanged(it)
            mentionViewModel.onDraftChanged(it)
        },
        onSendClicked = {
            viewModel.onSendClicked(reply?.messageId, mentionViewModel.mentionedIds(uiState.draft))
            replyViewModel.onReplyDropped()
            mentionViewModel.onSent()
        },
        mentions = mentions,
        onMentionSelected = { viewModel.onDraftChanged(mentionViewModel.onSelected(it, uiState.draft)) },
        search = ConversationSearch(
            state = searchState,
            onQueryChanged = searchViewModel::onQueryChanged,
            onLoadMore = searchViewModel::onLoadMore,
            onHitSelected = searchViewModel::onHitSelected,
            onJumpHandled = searchViewModel::onJumpHandled,
            onClosed = searchViewModel::onClosed,
            onJumpTo = searchViewModel::onJumpRequested,
        ),
        actions = bubbleActions,
        selection = MessageSelection(selectedIds, pins.canPin, pins.pinnedIds, selectionCallbacks),
        onAttachmentsPicked = viewModel::onAttachmentsPicked,
        onAttachmentRemoved = viewModel::onAttachmentRemoved,
        onAttachmentOpened = downloadsViewModel::onAttachmentOpened,
        onDownloadErrorShown = downloadsViewModel::onErrorShown,
        onLoadOlder = viewModel::onLoadOlder,
        onMuteChanged = actionsViewModel::onMuteChanged,
        onErrorShown = {
            viewModel.onErrorShown()
            actionsViewModel.onErrorShown()
            editViewModel.onErrorShown()
            pinsViewModel.onErrorShown()
        },
        modifier = modifier,
    )
}

@Composable
internal fun ConversationScreen(
    uiState: ConversationUiState,
    edit: MessageEditUiState,
    editActions: EditActions,
    reply: ReplyTarget?,
    onReplyDropped: () -> Unit,
    mentions: MentionUiState,
    onMentionSelected: (MentionCandidate) -> Unit,
    search: ConversationSearch,
    pins: List<PinnedMessage>,
    downloads: AttachmentDownloadsUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    actions: MessageActions,
    selection: MessageSelection,
    onAttachmentsPicked: (List<PickedAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    onAttachmentOpened: () -> Unit,
    onDownloadErrorShown: () -> Unit,
    onLoadOlder: () -> Unit,
    onMuteChanged: (Boolean) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notFoundMessage = stringResource(R.string.conversation_search_not_found)
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

    val copyMessage = rememberCopyMessage(snackbarHostState)
    var viewedImageId by rememberSaveable { mutableStateOf<String?>(null) }
    val viewedImage = uiState.messages.flatMap { it.attachments }.firstOrNull { it.id == viewedImageId }

    val searchBar: (@Composable () -> Unit)? = if (isSearching) {
        {
            ConversationSearchBar(search) {
                isSearching = false
                search.onClosed()
            }
        }
    } else {
        null
    }
    val messageActions = remember(actions) { actions.copy(onImageClick = { viewedImageId = it.id }) }
    val selectedMessages = rememberSelectedMessages(uiState.messages, selection)
    val title = uiState.title ?: uiState.type?.let { stringResource(it.fallbackTitleResource()) }.orEmpty()
    TitledScreen(
        title = title,
        modifier = modifier,
        subtitle = uiState.visibleTypingNames.takeIf { it.isNotEmpty() }?.let { typingText(it) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
        actions = {
            ConversationBarActions(uiState.isMuted, onSearch = { isSearching = true }, onMuteChanged = onMuteChanged)
        },
        searchBar = barInPlaceOfTitle(selectedMessages, selection, copyMessage, searchBar),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize()) {
            if (isSearching) {
                SearchResults(
                    state = search.state,
                    onHitSelected = {
                        search.onHitSelected(it)
                        isSearching = false
                        search.onClosed()
                    },
                    onLoadMore = search.onLoadMore,
                )
                return@Column
            }
            PinnedMessageBar(pins, onJump = search.onJumpTo)
            MessageList(
                uiState = uiState,
                downloads = downloads,
                actions = messageActions,
                selection = selection,
                jumpTo = search.state.jumpTo,
                onJumpHandled = search.onJumpHandled,
                onJumpFailed = {
                    search.onJumpHandled()
                    scope.launch { snackbarHostState.showSnackbar(notFoundMessage) }
                },
                onLoadOlder = onLoadOlder,
                modifier = Modifier.weight(1f),
            )
            ComposerArea(
                uiState = uiState,
                edit = edit,
                editActions = editActions,
                reply = reply,
                onReplyDropped = onReplyDropped,
                mentions = mentions,
                onMentionSelected = onMentionSelected,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked,
                onAttachmentsPicked = onAttachmentsPicked,
                onAttachmentRemoved = onAttachmentRemoved,
            )
        }
    }
    viewedImage?.let {
        val sender = uiState.messages.firstOrNull { message -> message.attachments.any { a -> a.id == it.id } }
        ImageViewer(
            attachment = it,
            title = viewerTitle(sender, it.fileName),
            subtitle = sender?.let { message -> viewerSubtitle(message.createdAtEpochMillis) },
            onDismiss = { viewedImageId = null },
            onOpenWithAnotherApp = { actions.onAttachmentClick(it) },
        )
    }
}

/** The selection bar, or else the search bar when it is open, takes the place of the title bar. */
private fun barInPlaceOfTitle(
    selected: List<Message>,
    selection: MessageSelection,
    onCopy: (String) -> Unit,
    searchBar: (@Composable () -> Unit)?,
): (@Composable () -> Unit)? = if (selected.isNotEmpty()) selectionBar(selected, selection, onCopy) else searchBar

@Composable
private fun MessageList(
    uiState: ConversationUiState,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
    selection: MessageSelection,
    jumpTo: String?,
    onJumpHandled: () -> Unit,
    onJumpFailed: () -> Unit,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val timeline = remember(uiState.messages, uiState.unreadMarker) {
        uiState.messages.withDateHeaders(ZoneId.systemDefault(), uiState.unreadMarker)
    }
    val hasUnreadDivider = uiState.unreadMarker != null
    val isNearOldestLoaded by remember { derivedStateOf { listState.isNearEnd() } }
    val isAtBottom by remember { derivedStateOf { listState.isAtNewest() } }
    val isScrolledUp by remember { derivedStateOf { listState.firstVisibleItemIndex >= SCROLLED_UP_ITEM_COUNT } }
    val newestMessage = uiState.messages.firstOrNull()
    var lastSeenId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isNearOldestLoaded, uiState.messages.size) {
        if (isNearOldestLoaded && uiState.messages.isNotEmpty()) onLoadOlder()
    }
    LaunchedEffect(isAtBottom, newestMessage?.id) {
        if (isAtBottom) lastSeenId = newestMessage?.id
    }
    LaunchedEffect(newestMessage?.id) {
        if (newestMessage?.isOwn == true || !isScrolledUp) listState.animateScrollToItem(0)
    }
    val highlight = rememberJumpHighlight()
    JumpToMessageEffect(
        jumpTo = jumpTo,
        timeline = timeline,
        isLoadingOlder = uiState.isLoadingOlder,
        isHistoryComplete = uiState.isHistoryComplete,
        listState = listState,
        highlight = highlight,
        onJumpHandled = onJumpHandled,
        onJumpFailed = onJumpFailed,
        onLoadOlder = onLoadOlder,
    )
    LaunchedEffect(hasUnreadDivider) {
        val index = timeline.indexOfFirst { it is TimelineItem.UnreadDivider }
        if (index >= 0) listState.showAtTop(index)
    }

    val currentTimeline by rememberUpdatedState(timeline)
    val jumpingActions = remember(actions) {
        actions.copy(
            onReplyClick = { id ->
                val index = currentTimeline.indexOfFirst { it is TimelineItem.Bubble && it.message.id == id }
                if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                highlight.show(id)
            },
        )
    }

    ShowTypingBubbleEffect(uiState.visibleTypingNames.isNotEmpty(), listState)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize()) {
            typingBubbleItem(uiState.visibleTypingNames)
            items(timeline, key = { it.key }) { item ->
                when (item) {
                    is TimelineItem.Bubble -> MessageBubble(
                        message = item.message,
                        modifier = Modifier.animateItem(),
                        showSenderName = uiState.showSenderNames,
                        downloads = downloads,
                        actions = jumpingActions,
                        startsGroup = item.startsGroup,
                        endsGroup = item.endsGroup,
                        isHighlighted = item.message.id == highlight.messageId,
                        selection = BubbleSelection(
                            isActive = selection.selectedIds.isNotEmpty(),
                            isSelected = item.message.id in selection.selectedIds,
                            onToggle = { selection.callbacks.onToggle(item.message.id) },
                        ),
                    )
                    is TimelineItem.DateHeader -> DateHeader(item.date, Modifier.animateItem())
                    is TimelineItem.UnreadDivider -> UnreadDivider(item.count, Modifier.animateItem())
                }
            }
            if (uiState.isLoadingOlder) {
                item(key = "loading-older") { LoadingOlderIndicator() }
            }
        }
        if (uiState.showEmptyMessage) {
            EmptyState(
                R.drawable.illustration_conversation,
                R.string.conversation_empty,
                R.string.conversation_empty_hint,
            )
        }
        AnimatedVisibility(
            visible = isScrolledUp,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            ScrollToLatestButton(
                unseenCount = unseenIncomingCount(uiState.messages, lastSeenId),
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
            )
        }
    }
}

/** Scrolls so the item at [index] is at the top of the list, which shows everything newer below it. */
private suspend fun LazyListState.showAtTop(index: Int) {
    scrollToItem(index)
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    scrollBy(-(viewport - item.size - item.offset).toFloat().coerceAtLeast(0f))
}

private fun LazyListState.isAtNewest(): Boolean = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

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
    ConversationError.TOO_MANY_PINS -> R.string.pin_error_too_many
    ConversationError.MESSAGE_TOO_LONG -> R.string.conversation_error_message_too_long
    ConversationError.NOT_SIGNED_IN -> R.string.conversation_error_not_signed_in
    ConversationError.CONVERSATION_UNAVAILABLE -> R.string.conversation_error_unavailable
    ConversationError.TOO_MANY_ATTACHMENTS -> R.string.conversation_error_too_many_attachments
    ConversationError.ATTACHMENT_TOO_LARGE -> R.string.conversation_error_attachment_too_large
    ConversationError.ATTACHMENT_TYPE_NOT_ALLOWED -> R.string.conversation_error_attachment_type
    ConversationError.ATTACHMENT_UNREADABLE -> R.string.conversation_error_attachment_unreadable
    ConversationError.DOWNLOAD_NETWORK_UNAVAILABLE -> R.string.conversation_error_download_network_unavailable
    ConversationError.DOWNLOAD_UNKNOWN -> R.string.conversation_error_download_unknown
}

@Composable
private fun rememberCopyMessage(snackbarHostState: SnackbarHostState): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.message_copied)
    return remember(clipboard, scope, copiedMessage, snackbarHostState) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, text)))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbarHostState.showSnackbar(copiedMessage)
            }
        }
    }
}
