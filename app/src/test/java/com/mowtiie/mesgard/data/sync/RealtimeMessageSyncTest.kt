package com.mowtiie.mesgard.data.sync

import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.remote.MessageRealtimeSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.RealtimeEvent
import com.mowtiie.mesgard.data.remote.SenderDto
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.attachmentDto
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageDto
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RealtimeMessageSyncTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val source = FakeRealtimeSource()
    private val profileSource = mockk<ProfileRemoteSource> { coEvery { fetch(any()) } returns null }
    private val messageSource = mockk<MessageRemoteSource> {
        coEvery { fetchAttachments(any()) } returns emptyList()
    }
    private val refreshed = CompletableDeferred<Unit>()
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { refresh() } coAnswers {
            refreshed.complete(Unit)
            RefreshResult.Success
        }
    }
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val sync by lazy {
        RealtimeMessageSync(
            source,
            messageSource,
            profileSource,
            databaseRule.database,
            conversationRepository,
            authRepository,
        )
    }

    @Before
    fun storeConversationAndProfile() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity(CONVERSATION_ID)))
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity(OTHER_USER_ID, "Ana")))
    }

    @Test
    fun `stores an incoming message and counts it as unread`() = runTest {
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1")))

        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 1 }
        assertEquals("m1", databaseRule.database.messageDao().findById("m1")?.id)
    }

    @Test
    fun `does not count a message sent by the current user as unread`() = runTest {
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("mine", senderId = CURRENT_USER_ID)))
        source.emit(RealtimeEvent.MessageInserted(incoming("theirs")))

        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 1 }
    }

    @Test
    fun `counts a repeated insert only once`() = runTest {
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1")))
        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 1 }
        source.emit(RealtimeEvent.MessageInserted(incoming("m1")))
        source.emit(RealtimeEvent.MessageInserted(incoming("m2")))

        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 2 }
    }

    @Test
    fun `applies an update to a stored message without counting it as unread`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("m1", CONVERSATION_ID, 1_000L))
        startSync()

        source.emit(RealtimeEvent.MessageUpdated(incoming("m1", deletedAt = "2026-11-10T09:00:00+00:00")))

        awaitValue(databaseRule.database.messageDao().observeTimeline(CONVERSATION_ID, PAGE)) {
            it.singleOrNull()?.deletedAtEpochMillis != null
        }
        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 0 }
    }

    @Test
    fun `fetches the attachments of an incoming message that has them`() = runTest {
        coEvery { messageSource.fetchAttachments("m1") } returns listOf(attachmentDto("a1"))
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1", hasAttachments = true)))

        awaitValue(databaseRule.database.attachmentDao().observeForConversation(CONVERSATION_ID)) {
            it.singleOrNull()?.id == "a1"
        }
    }

    @Test
    fun `does not fetch attachments for a text message`() = runTest {
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1")))

        awaitValue(databaseRule.database.conversationDao().observeById(CONVERSATION_ID)) { it?.unreadCount == 1 }
        coVerify(exactly = 0) { messageSource.fetchAttachments(any()) }
    }

    @Test
    fun `refreshes the conversation list when a message belongs to an unknown conversation`() = runTest {
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1", conversationId = "new-conversation")))

        withContext(Dispatchers.Default) { withTimeout(TIMEOUT_MS) { refreshed.await() } }
    }

    @Test
    fun `fetches the profile of an unknown sender`() = runTest {
        coEvery { profileSource.fetch("stranger") } returns SenderDto(id = "stranger", displayName = "Ben")
        startSync()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1", senderId = "stranger", senderName = null)))

        awaitValue(databaseRule.database.messageDao().observeTimeline(CONVERSATION_ID, PAGE)) {
            it.singleOrNull()?.senderName == "Ben"
        }
    }

    @Test
    fun `does not treat the first connection as a reconnection`() = runTest {
        startSync()

        source.emit(RealtimeEvent.Connected)
        runCurrent()

        coVerify(exactly = 0) { conversationRepository.refresh() }
    }

    @Test
    fun `refreshes the conversations and announces a reconnection when the connection is restored`() = runTest {
        val reconnected = CompletableDeferred<Unit>()
        startSync()
        backgroundScope.launch { sync.reconnections.collect { reconnected.complete(Unit) } }
        runCurrent()

        source.emit(RealtimeEvent.Connected)
        source.emit(RealtimeEvent.Connected)
        runCurrent()

        withContext(Dispatchers.Default) { withTimeout(TIMEOUT_MS) { reconnected.await() } }
        coVerify(exactly = 1) { conversationRepository.refresh() }
    }

    @Test
    fun `subscribes only once when started twice`() = runTest {
        sync.start(backgroundScope)
        sync.start(backgroundScope)
        runCurrent()

        assertEquals(1, source.subscriptions)
    }

    @Test
    fun `ignores events after it is stopped`() = runTest {
        startSync()
        sync.stop()
        runCurrent()

        source.emit(RealtimeEvent.MessageInserted(incoming("m1")))
        runCurrent()

        assertNull(databaseRule.database.messageDao().findById("m1"))
    }

    private fun TestScope.startSync() {
        sync.start(backgroundScope)
        runCurrent()
    }

    private suspend fun <T> awaitValue(flow: Flow<T>, predicate: (T) -> Boolean): T =
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT_MS) { flow.first(predicate) } }

    private fun incoming(
        id: String,
        senderId: String = OTHER_USER_ID,
        conversationId: String = CONVERSATION_ID,
        senderName: String? = "Ana",
        deletedAt: String? = null,
        hasAttachments: Boolean = false,
    ) = messageDto(
        id,
        conversationId,
        senderId = senderId,
        senderName = senderName,
        deletedAt = deletedAt,
        hasAttachments = hasAttachments,
    )

    private class FakeRealtimeSource : MessageRealtimeSource {
        private val events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = EVENT_BUFFER)
        var subscriptions = 0
            private set

        override fun events(): Flow<RealtimeEvent> = events.onStart { subscriptions++ }

        suspend fun emit(event: RealtimeEvent) = events.emit(event)
    }

    private companion object {
        const val CONVERSATION_ID = "chat"
        const val CURRENT_USER_ID = "me"
        const val OTHER_USER_ID = "other"
        const val PAGE = 50
        const val TIMEOUT_MS = 5_000L
        const val EVENT_BUFFER = 16
    }
}
