package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.ConversationPreferencesRemoteSource
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstConversationPreferencesRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remote = mockk<ConversationPreferencesRemoteSource>(relaxed = true)
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val messageRepository = mockk<MessageRepository> {
        coEvery { markConversationRead(any()) } returns RefreshResult.Success
    }
    private val repository by lazy {
        OfflineFirstConversationPreferencesRepository(databaseRule.database, remote, authRepository, messageRepository)
    }
    private val dao get() = databaseRule.database.conversationDao()
    private val preferencesDao get() = databaseRule.database.conversationPreferencesDao()

    @Before
    fun storeConversations() = runTest {
        dao.upsertAll(listOf(conversationEntity("a"), conversationEntity("b"), conversationEntity("c")))
    }

    private suspend fun preferences(id: String) = preferencesDao.find(listOf(id)).single()

    @Test
    fun `pinning is applied on the phone and sent to the server in one request`() = runTest {
        val changes = slot<JsonObject>()
        coEvery { remote.update("me", any(), capture(changes)) } returns Unit

        assertEquals(PreferenceResult.Success, repository.setPinned(setOf("a", "b"), true))

        assertNotNull(preferences("a").pinnedAt)
        assertNotNull(preferences("b").pinnedAt)
        coVerify(exactly = 1) { remote.update("me", setOf("a", "b"), any()) }
        assertTrue(changes.captured["pinned_at"] is JsonPrimitive)
    }

    @Test
    fun `a fourth pin is refused before anything is sent`() = runTest {
        dao.upsertAll(
            listOf(
                conversationEntity("p1", pinnedAt = 1L),
                conversationEntity("p2", pinnedAt = 2L),
                conversationEntity("p3", pinnedAt = 3L),
            ),
        )

        val result = repository.setPinned(setOf("a"), true)

        assertEquals(PreferenceResult.Failure(PreferenceError.TOO_MANY_PINNED), result)
        assertNull(preferences("a").pinnedAt)
        coVerify(exactly = 0) { remote.update(any(), any(), any()) }
    }

    @Test
    fun `pinning one that is already pinned does not count twice`() = runTest {
        dao.upsertAll(
            listOf(
                conversationEntity("p1", pinnedAt = 1L),
                conversationEntity("p2", pinnedAt = 2L),
                conversationEntity("p3", pinnedAt = 3L),
            ),
        )

        assertEquals(PreferenceResult.Success, repository.setPinned(setOf("p3"), true))
    }

    @Test
    fun `unpinning sends a null`() = runTest {
        dao.upsertAll(listOf(conversationEntity("p1", pinnedAt = 1L)))
        val changes = slot<JsonObject>()
        coEvery { remote.update("me", any(), capture(changes)) } returns Unit

        repository.setPinned(setOf("p1"), false)

        assertNull(preferences("p1").pinnedAt)
        assertEquals(JsonNull, changes.captured["pinned_at"])
    }

    @Test
    fun `when the server cannot be reached the earlier state comes back`() = runTest {
        coEvery { remote.update(any(), any(), any()) } throws IOException("offline")

        val result = repository.setArchived(setOf("a"), true)

        assertEquals(PreferenceResult.Failure(PreferenceError.NETWORK_UNAVAILABLE), result)
        assertNull(preferences("a").archivedAt)
    }

    @Test
    fun `muting and marking as unread are applied to the whole selection`() = runTest {
        repository.setMuted(setOf("a", "b"), true)
        repository.markUnread(setOf("b", "c"))

        assertTrue(preferences("a").isMuted)
        assertTrue(preferences("b").isMuted && preferences("b").markedUnread)
        assertTrue(preferences("c").markedUnread)
    }

    @Test
    fun `marking as read clears the flag and reads each conversation`() = runTest {
        repository.markUnread(setOf("a", "b"))

        assertEquals(PreferenceResult.Success, repository.markRead(setOf("a", "b")))

        assertEquals(false, preferences("a").markedUnread)
        coVerify { messageRepository.markConversationRead("a") }
        coVerify { messageRepository.markConversationRead("b") }
    }

    @Test
    fun `deleting for me hides the conversation after its newest message and sends the time`() = runTest {
        databaseRule.database.messageDao().upsertAll(listOf(messageEntity("m1", "a", createdAt = 5_000L)))
        preferencesDao.updatePinned(listOf("a"), 1L)
        val changes = slot<JsonObject>()
        coEvery { remote.update("me", listOf("a"), capture(changes)) } returns Unit

        assertEquals(PreferenceResult.Success, repository.deleteForMe(setOf("a")))

        assertEquals(5_000L, preferences("a").hiddenBefore)
        assertNull(preferences("a").pinnedAt)
        assertEquals(JsonNull, changes.captured["pinned_at"])
        assertEquals(JsonPrimitive("1970-01-01T00:00:05Z"), changes.captured["hidden_before"])
    }

    @Test
    fun `a refused delete brings the conversation back`() = runTest {
        coEvery { remote.update(any(), any(), any()) } throws IOException("offline")

        val result = repository.deleteForMe(setOf("a"))

        assertTrue(result is PreferenceResult.Failure)
        assertNull(preferences("a").hiddenBefore)
    }
}
