package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENTS_PER_MESSAGE
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.Message
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
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversation = MutableStateFlow<ConversationSummary?>(summary())
    private val messages = MutableStateFlow(emptyList<Message>())
    private val conversationRepository = mockk<ConversationRepository> {
        every { observeConversation(CONVERSATION_ID) } returns conversation
    }
    private val reconnections = MutableSharedFlow<Unit>()
    private val realtimeSync = mockk<RealtimeSync> {
        every { this@mockk.reconnections } returns
            this@ConversationViewModelTest.reconnections
    }
    private val typingNames = MutableStateFlow(emptyList<String>())
    private lateinit var ownTyping: Flow<Unit>
    private val typingRepository = mockk<TypingRepository> {
        every { observeTypingNames(CONVERSATION_ID, any()) } answers {
            ownTyping = secondArg()
            typingNames
        }
    }
    private val messageRepository = mockk<MessageRepository> {
        every { observeMessages(CONVERSATION_ID, any()) } returns messages
        coEvery { refresh(CONVERSATION_ID) } returns RefreshResult.Success
        coEvery { markConversationRead(CONVERSATION_ID) } returns RefreshResult.Success
    }

    @Test
    fun `exposes the conversation details and its messages`() = runTest {
        messages.value = listOf(message("m1"))

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Study group", state.title)
            assertEquals(ConversationType.GROUP, state.type)
            assertEquals(listOf("m1"), state.messages.map { it.id })
            assertTrue(state.isLoaded)
        }
    }

    @Test
    fun `refreshes the messages when it is created`() = runTest {
        viewModel().uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { messageRepository.refresh(CONVERSATION_ID) }
    }

    @Test
    fun `reports a refresh failure and clears it once shown`() = runTest {
        coEvery { messageRepository.refresh(CONVERSATION_ID) } returns
            RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ConversationError.NETWORK_UNAVAILABLE, expectMostRecentItem().error)
            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `shows sender names in group and announcement conversations but not in direct ones`() = runTest {
        conversation.value = summary(type = ConversationType.DIRECT)
        viewModel().uiState.test { assertFalse(expectMostRecentItem().showSenderNames) }

        conversation.value = summary(type = ConversationType.ANNOUNCEMENT)
        viewModel().uiState.test { assertTrue(expectMostRecentItem().showSenderNames) }
    }

    @Test
    fun `reports whether the user can post`() = runTest {
        conversation.value = summary(canPost = false)

        viewModel().uiState.test { assertFalse(expectMostRecentItem().canPost) }
    }

    @Test
    fun `shows the empty message once loaded without messages`() = runTest {
        viewModel().uiState.test { assertTrue(expectMostRecentItem().showEmptyMessage) }
    }

    @Test
    fun `cannot send a blank draft`() = runTest {
        val viewModel = viewModel()

        viewModel.onDraftChanged("   ")

        viewModel.uiState.test { assertFalse(expectMostRecentItem().canSend) }
    }

    @Test
    fun `cannot send a draft that is longer than the limit`() = runTest {
        val viewModel = viewModel()

        viewModel.onDraftChanged("a".repeat(MAX_LENGTH + 1))

        viewModel.uiState.test { assertFalse(expectMostRecentItem().canSend) }
    }

    @Test
    fun `sends the draft and clears it once it is queued`() = runTest {
        coEvery { messageRepository.send(CONVERSATION_ID, "Hello") } returns SendResult.Queued
        val viewModel = viewModel()
        viewModel.onDraftChanged("Hello")

        viewModel.onSendClicked()

        coVerify(exactly = 1) { messageRepository.send(CONVERSATION_ID, "Hello") }
        viewModel.uiState.test { assertEquals("", expectMostRecentItem().draft) }
    }

    @Test
    fun `keeps the draft and reports the reason when sending is rejected`() = runTest {
        coEvery { messageRepository.send(any(), any()) } returns
            SendResult.Rejected(SendRejection.MESSAGE_TOO_LONG)
        val viewModel = viewModel()
        viewModel.onDraftChanged("Hello")

        viewModel.onSendClicked()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Hello", state.draft)
            assertEquals(ConversationError.MESSAGE_TOO_LONG, state.error)
        }
    }

    @Test
    fun `does not send a blank draft`() = runTest {
        val viewModel = viewModel()
        viewModel.onDraftChanged("  ")

        viewModel.onSendClicked()

        coVerify(exactly = 0) { messageRepository.send(any(), any()) }
    }

    @Test
    fun `sends the picked attachments with the draft and clears them once queued`() = runTest {
        coEvery { messageRepository.send(CONVERSATION_ID, "Hello", listOf("content://a")) } returns SendResult.Queued
        val viewModel = viewModel()
        viewModel.onDraftChanged("Hello")
        viewModel.onAttachmentsPicked(listOf(PickedAttachment("content://a", "a.pdf")))

        viewModel.onSendClicked()

        coVerify(exactly = 1) { messageRepository.send(CONVERSATION_ID, "Hello", listOf("content://a")) }
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("", state.draft)
            assertEquals(emptyList<PickedAttachment>(), state.attachments)
        }
    }

    @Test
    fun `can send attachments without any text`() = runTest {
        val viewModel = viewModel()
        viewModel.onAttachmentsPicked(listOf(PickedAttachment("content://a", "a.pdf")))

        viewModel.uiState.test { assertTrue(expectMostRecentItem().canSend) }
    }

    @Test
    fun `removes a picked attachment`() = runTest {
        val viewModel = viewModel()
        viewModel.onAttachmentsPicked(listOf(PickedAttachment("content://a", "a.pdf"), PickedAttachment("b", "b.pdf")))

        viewModel.onAttachmentRemoved("content://a")

        viewModel.uiState.test { assertEquals(listOf("b"), expectMostRecentItem().attachments.map { it.uri }) }
    }

    @Test
    fun `refuses more attachments than a message can carry and keeps the ones already picked`() = runTest {
        val viewModel = viewModel()
        viewModel.onAttachmentsPicked(listOf(PickedAttachment("first", "first")))

        viewModel.onAttachmentsPicked(List(MAX_ATTACHMENTS_PER_MESSAGE) { PickedAttachment("more$it", "more$it") })

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("first"), state.attachments.map { it.uri })
            assertEquals(ConversationError.TOO_MANY_ATTACHMENTS, state.error)
        }
    }

    @Test
    fun `retries a failed message through the repository`() = runTest {
        coEvery { messageRepository.retry("failed") } returns Unit

        viewModel().onRetryClicked("failed")

        coVerify(exactly = 1) { messageRepository.retry("failed") }
    }

    @Test
    fun `loading older messages widens the window and fetches the previous page`() = runTest {
        coEvery { messageRepository.loadOlder(CONVERSATION_ID) } returns PageResult.Loaded(endReached = false)
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLoadOlder()
            cancelAndIgnoreRemainingEvents()
        }

        verify { messageRepository.observeMessages(CONVERSATION_ID, MESSAGE_PAGE_SIZE * 2) }
        coVerify(exactly = 1) { messageRepository.loadOlder(CONVERSATION_ID) }
    }

    @Test
    fun `stops fetching older messages once the history is complete`() = runTest {
        coEvery { messageRepository.loadOlder(CONVERSATION_ID) } returns PageResult.Loaded(endReached = true)
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLoadOlder()
            viewModel.onLoadOlder()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { messageRepository.loadOlder(CONVERSATION_ID) }
    }

    @Test
    fun `reports that older messages are loading and ignores a second request meanwhile`() = runTest {
        val pending = CompletableDeferred<PageResult>()
        coEvery { messageRepository.loadOlder(CONVERSATION_ID) } coAnswers { pending.await() }
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLoadOlder()
            viewModel.onLoadOlder()
            assertTrue(expectMostRecentItem().isLoadingOlder)
            pending.complete(PageResult.Loaded(endReached = false))
            assertFalse(expectMostRecentItem().isLoadingOlder)
        }

        coVerify(exactly = 1) { messageRepository.loadOlder(CONVERSATION_ID) }
    }

    @Test
    fun `reports a failure to load older messages`() = runTest {
        coEvery { messageRepository.loadOlder(CONVERSATION_ID) } returns
            PageResult.Failure(RefreshError.UNKNOWN)
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLoadOlder()
            val state = expectMostRecentItem()
            assertEquals(ConversationError.UNKNOWN, state.error)
            assertFalse(state.isLoadingOlder)
        }
    }

    @Test
    fun `refreshes the messages again when the realtime connection is restored`() = runTest {
        viewModel()

        reconnections.emit(Unit)

        coVerify(exactly = 2) { messageRepository.refresh(CONVERSATION_ID) }
    }

    @Test
    fun `marks the conversation as read when a new incoming message arrives while visible`() = runTest {
        viewModel().onVisibilityChanged(true)

        messages.value = listOf(message("incoming"))

        coVerify(exactly = 1) { messageRepository.markConversationRead(CONVERSATION_ID) }
    }

    @Test
    fun `marks the conversation as read once for the same incoming message`() = runTest {
        viewModel().onVisibilityChanged(true)

        messages.value = listOf(message("incoming"))
        messages.value = listOf(message("mine", isOwn = true), message("incoming"))

        coVerify(exactly = 1) { messageRepository.markConversationRead(CONVERSATION_ID) }
    }

    @Test
    fun `does not mark the conversation as read while the screen is hidden`() = runTest {
        viewModel()

        messages.value = listOf(message("incoming"))

        coVerify(exactly = 0) { messageRepository.markConversationRead(any()) }
    }

    @Test
    fun `marks messages that arrived while hidden once the screen becomes visible`() = runTest {
        val viewModel = viewModel()
        messages.value = listOf(message("incoming"))
        coVerify(exactly = 0) { messageRepository.markConversationRead(any()) }

        viewModel.onVisibilityChanged(true)

        coVerify(exactly = 1) { messageRepository.markConversationRead(CONVERSATION_ID) }
    }

    @Test
    fun `does not mark the conversation as read again when the screen returns without new messages`() = runTest {
        val viewModel = viewModel()
        viewModel.onVisibilityChanged(true)
        messages.value = listOf(message("incoming"))

        viewModel.onVisibilityChanged(false)
        viewModel.onVisibilityChanged(true)

        coVerify(exactly = 1) { messageRepository.markConversationRead(CONVERSATION_ID) }
    }

    @Test
    fun `marks the conversation as read for a message that arrives after the screen was hidden and shown`() = runTest {
        val viewModel = viewModel()
        viewModel.onVisibilityChanged(true)
        messages.value = listOf(message("first"))
        viewModel.onVisibilityChanged(false)
        messages.value = listOf(message("second"), message("first"))

        coVerify(exactly = 1) { messageRepository.markConversationRead(CONVERSATION_ID) }
        viewModel.onVisibilityChanged(true)

        coVerify(exactly = 2) { messageRepository.markConversationRead(CONVERSATION_ID) }
    }

    @Test
    fun `does not mark the conversation as read for own or unsent messages`() = runTest {
        viewModel().onVisibilityChanged(true)

        messages.value = listOf(
            message("mine", isOwn = true),
            message("pending", status = MessageStatus.PENDING),
        )

        coVerify(exactly = 0) { messageRepository.markConversationRead(any()) }
    }

    @Test
    fun `exposes who is typing`() = runTest {
        typingNames.value = listOf("Ana")

        viewModel().uiState.test { assertEquals(listOf("Ana"), expectMostRecentItem().typingNames) }
    }

    @Test
    fun `signals typing when the draft has text`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            ownTyping.test {
                viewModel.onDraftChanged("Hel")
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not signal typing when the draft is cleared`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            ownTyping.test {
                viewModel.onDraftChanged("   ")
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel() = ConversationViewModel(
        savedStateHandle = SavedStateHandle(mapOf("conversationId" to CONVERSATION_ID)),
        conversationRepository = conversationRepository,
        messageRepository = messageRepository,
        typingRepository = typingRepository,
        realtimeSync = realtimeSync,
    )

    private fun summary(type: ConversationType = ConversationType.GROUP, canPost: Boolean = true) = ConversationSummary(
        id = CONVERSATION_ID,
        type = type,
        title = "Study group",
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 0L,
        unreadCount = 0,
        canPost = canPost,
    )

    private fun message(id: String, isOwn: Boolean = false, status: MessageStatus = MessageStatus.SENT) = Message(
        id = id,
        conversationId = CONVERSATION_ID,
        senderId = if (isOwn) "me" else "other",
        senderName = "Ana",
        body = "Hello",
        createdAtEpochMillis = 1_000L,
        isEdited = false,
        isDeleted = false,
        status = status,
        isOwn = isOwn,
    )

    private companion object {
        const val CONVERSATION_ID = "chat"
        const val MAX_LENGTH = 4000
    }
}
