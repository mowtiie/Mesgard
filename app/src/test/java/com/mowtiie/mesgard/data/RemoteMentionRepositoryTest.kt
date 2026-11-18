package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MemberNameDto
import com.mowtiie.mesgard.data.remote.MentionRemoteSource
import com.mowtiie.mesgard.data.remote.ParticipantNameDto
import com.mowtiie.mesgard.data.remote.ReaderProfileDto
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MENTION_SUGGESTION_LIMIT
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteMentionRepositoryTest {
    private val remote = mockk<MentionRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val repository = RemoteMentionRepository(remote, authRepository)

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
}
