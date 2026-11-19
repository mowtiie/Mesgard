package com.mowtiie.mesgard.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/** The announcements of the organization: the messages of its announcement conversation, shown as a feed. */
@HiltViewModel
internal class AnnouncementsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {
    private val refreshState = MutableStateFlow(RefreshState())
    private val isVisible = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnnouncementsUiState> = combine(
        conversationRepository.observeAnnouncements().flatMapLatest { conversation ->
            if (conversation == null) {
                flowOf(Feed(null, emptyList()))
            } else {
                messageRepository.observeMessages(conversation.id, MESSAGE_PAGE_SIZE)
                    .map { messages -> Feed(conversation, messages) }
            }
        },
        refreshState,
    ) { (conversation, messages), refresh ->
        AnnouncementsUiState(
            conversationId = conversation?.id,
            announcements = messages.filterNot { it.isDeleted },
            unreadCount = conversation?.unreadCount ?: 0,
            canPost = conversation?.canPost ?: false,
            isLoaded = true,
            isRefreshing = refresh.isRefreshing,
            refreshError = refresh.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = AnnouncementsUiState(),
    )

    init {
        onRefresh()
        viewModelScope.launch {
            combine(isVisible, uiState) { visible, state -> state.takeIf { visible && it.unreadCount > 0 } }
                .collect { state -> state?.conversationId?.let { messageRepository.markConversationRead(it) } }
        }
    }

    fun onRefresh() {
        if (refreshState.value.isRefreshing) return
        refreshState.update { RefreshState(isRefreshing = true) }
        viewModelScope.launch {
            val result = conversationRepository.refresh()
            val conversationId = uiState.value.conversationId
            val messages = if (result is RefreshResult.Success && conversationId != null) {
                messageRepository.refresh(conversationId)
            } else {
                result
            }
            refreshState.update { RefreshState(error = (messages as? RefreshResult.Failure)?.error) }
        }
    }

    fun onVisibilityChanged(visible: Boolean) {
        isVisible.value = visible
    }

    fun onRefreshErrorShown() {
        refreshState.update { it.copy(error = null) }
    }

    private data class Feed(val conversation: ConversationSummary?, val messages: List<Message>)

    private data class RefreshState(val isRefreshing: Boolean = false, val error: RefreshError? = null)
}
