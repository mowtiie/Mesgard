package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AnnouncementRemoteSource
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteAnnouncementRepositoryTest {
    private val remote = mockk<AnnouncementRemoteSource>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val repository = RemoteAnnouncementRepository(remote, conversationRepository)

    @Test
    fun `creates the conversation and stores it on the device before returning its id`() = runTest {
        coEvery { remote.createAnnouncementConversation(ORG) } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Success

        assertEquals(AdminResult.Success("conversation-1"), repository.openAnnouncements(ORG))
        coVerifyOrder {
            remote.createAnnouncementConversation(ORG)
            conversationRepository.refresh()
        }
    }

    @Test
    fun `does not open a conversation that could not be stored`() = runTest {
        coEvery { remote.createAnnouncementConversation(ORG) } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.openAnnouncements(ORG))
    }

    @Test
    fun `reports a refusal or a lost connection when creating the conversation`() = runTest {
        coEvery { remote.createAnnouncementConversation(ORG) } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.openAnnouncements(ORG))
    }

    private companion object {
        const val ORG = "org"
    }
}
