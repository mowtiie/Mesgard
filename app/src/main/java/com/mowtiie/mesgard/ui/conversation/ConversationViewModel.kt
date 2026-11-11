package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SendRejection
import com.mowtiie.mesgard.domain.model.SendResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.domain.repository.TypingRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    typingRepository: TypingRepository,
    realtimeSync: RealtimeSync,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val visibleLimit = MutableStateFlow(MESSAGE_PAGE_SIZE)
    private val localState = MutableStateFlow(LocalState())
    private val ownTyping =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ConversationUiState> = combine(
        conversationRepository.observeConversation(conversationId),
        visibleLimit.flatMapLatest { messageRepository.observeMessages(conversationId, it) },
        typingRepository.observeTypingNames(conversationId, ownTyping).onStart { emit(emptyList()) },
        localState,
    ) { conversation, messages, typingNames, local ->
        ConversationUiState(
            title = conversation?.title,
            type = conversation?.type,
            canPost = conversation?.canPost ?: true,
            messages = messages,
            draft = local.draft,
            typingNames = typingNames,
            isLoaded = true,
            isLoadingOlder = local.isLoadingOlder,
            error = local.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = ConversationUiState(),
    )

    init {
        viewModelScope.launch { reportFailure(messageRepository.refresh(conversationId)) }
        viewModelScope.launch { markReadWhenNewMessagesArrive() }
        viewModelScope.launch {
            realtimeSync.reconnections.collect { reportFailure(messageRepository.refresh(conversationId)) }
        }
    }

    fun onDraftChanged(draft: String) {
        localState.update { it.copy(draft = draft) }
        if (draft.isNotBlank()) ownTyping.tryEmit(Unit)
    }

    fun onSendClicked() {
        val draft = localState.value.draft
        if (draft.isBlank()) return
        viewModelScope.launch {
            when (val result = messageRepository.send(conversationId, draft)) {
                SendResult.Queued -> localState.update { it.copy(draft = "") }
                is SendResult.Rejected -> localState.update { it.copy(error = result.reason.toError()) }
            }
        }
    }

    fun onRetryClicked(messageId: String) {
        viewModelScope.launch { messageRepository.retry(messageId) }
    }

    fun onLoadOlder() {
        val state = localState.value
        if (state.isLoadingOlder) return
        localState.update { it.copy(isLoadingOlder = true) }
        visibleLimit.update { it + MESSAGE_PAGE_SIZE }
        viewModelScope.launch {
            if (!state.isHistoryComplete) {
                when (val result = messageRepository.loadOlder(conversationId)) {
                    is PageResult.Loaded -> localState.update { it.copy(isHistoryComplete = result.endReached) }
                    is PageResult.Failure -> localState.update { it.copy(error = result.error.toError()) }
                }
            }
            localState.update { it.copy(isLoadingOlder = false) }
        }
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private suspend fun markReadWhenNewMessagesArrive() {
        messageRepository.observeMessages(conversationId, MESSAGE_PAGE_SIZE)
            .map { messages -> messages.firstOrNull { !it.isOwn && it.status == MessageStatus.SENT }?.id }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { messageRepository.markConversationRead(conversationId) }
    }

    private fun reportFailure(result: RefreshResult) {
        if (result is RefreshResult.Failure) localState.update { it.copy(error = result.error.toError()) }
    }

    private data class LocalState(
        val draft: String = "",
        val isLoadingOlder: Boolean = false,
        val isHistoryComplete: Boolean = false,
        val error: ConversationError? = null,
    )
}

private fun RefreshError.toError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> ConversationError.NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> ConversationError.UNKNOWN
}

private fun SendRejection.toError() = when (this) {
    SendRejection.EMPTY_MESSAGE -> ConversationError.UNKNOWN
    SendRejection.MESSAGE_TOO_LONG -> ConversationError.MESSAGE_TOO_LONG
    SendRejection.NOT_SIGNED_IN -> ConversationError.NOT_SIGNED_IN
    SendRejection.CONVERSATION_UNAVAILABLE -> ConversationError.CONVERSATION_UNAVAILABLE
}
