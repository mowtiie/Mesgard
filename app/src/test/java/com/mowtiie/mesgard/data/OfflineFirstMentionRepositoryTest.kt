package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.remote.MemberNameDto
import com.mowtiie.mesgard.data.remote.MentionRemoteSource
import com.mowtiie.mesgard.data.remote.ParticipantNameDto
import com.mowtiie.mesgard.data.remote.ReaderProfileDto
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MENTION_SUGGESTION_LIMIT
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstMentionRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remote = mockk<MentionRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val repository by lazy { OfflineFirstMentionRepository(remote, databaseRule.database, authRepository) }

    private fun participant(id: String, name: String) = ParticipantNameDto(id, MemberNameDto(ReaderProfileDto(name)))

    @Test
    fun `suggests the people whose name matches, without the current user`() = runTest {
        coEvery { remote.searchParticipants("c1", "an", MENTION_SUGGESTION_LIMIT + 1) } returns
            listOf(participant("me", "Ana Me"), participant("u1", "Ana Cruz"))

        assertEquals(listOf(MentionCandidate("u1", "Ana Cruz")), repository.search("c1", "an"))
    }

    @Test
    fun `shows no more than the limit`() = runTest {
        coEvery { remote.searchParticipants(any(), any(), any()) } returns
            (1..MENTION_SUGGESTION_LIMIT + 1).map { participant("u$it", "Person $it") }

        assertEquals(MENTION_SUGGESTION_LIMIT, repository.search("c1", "p").size)
    }

    @Test
    fun `a connection problem gives no suggestions and does not stop anyone from writing`() = runTest {
        coEvery { remote.searchParticipants(any(), any(), any()) } throws IOException("offline")

        assertEquals(emptyList<MentionCandidate>(), repository.search("c1", "an"))
    }

    @Test
    fun `finds the people who wrote in the conversation without the server`() = runTest {
        val database = databaseRule.database
        database.conversationDao().upsertAll(listOf(conversationEntity("c1")))
        database.profileDao().upsertAll(
            listOf(ProfileEntity("u1", "Ana Cruz"), ProfileEntity("u2", "Ben Ana"), ProfileEntity("me", "Ana Me")),
        )
        database.messageDao().upsertAll(
            listOf(
                messageEntity("m1", "c1", 1_000L).copy(senderId = "u1"),
                messageEntity("m2", "c1", 2_000L).copy(senderId = "u1"),
                messageEntity("m3", "c1", 3_000L).copy(senderId = "u2"),
                messageEntity("m4", "c1", 4_000L).copy(senderId = "me"),
            ),
        )

        assertEquals(
            listOf(MentionCandidate("u1", "Ana Cruz"), MentionCandidate("u2", "Ben Ana")),
            repository.searchStored("c1", "ana"),
        )
        assertEquals(emptyList<MentionCandidate>(), repository.searchStored("c1", "zzz"))
    }
}
