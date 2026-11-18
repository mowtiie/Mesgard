package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MessageSearchRemoteSource
import com.mowtiie.mesgard.data.remote.SearchHitDto
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.SearchCursor
import com.mowtiie.mesgard.domain.model.SearchOutcome
import com.mowtiie.mesgard.domain.repository.SEARCH_PAGE_SIZE
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteConversationSearchRepositoryTest {
    private val remote = mockk<MessageSearchRemoteSource>()
    private val repository = RemoteConversationSearchRepository(remote)

    private fun row(number: Int) = SearchHitDto(
        messageId = "m$number",
        senderName = "Ana",
        body = "Due on Friday",
        createdAt = "2026-11-14T16:56:%02d.123456+00:00".format(number),
    )

    @Test
    fun `reads the hits with their time`() = runTest {
        coEvery { remote.search("c1", "due", null, SEARCH_PAGE_SIZE) } returns listOf(row(5))

        val page = (repository.search("c1", "due", null) as SearchOutcome.Loaded).page

        assertEquals("m5", page.hits.single().messageId)
        assertEquals(1_794_675_365_123L, page.hits.single().createdAtEpochMillis)
        assertNull(page.next)
    }

    @Test
    fun `a full page points to the next one after its last hit`() = runTest {
        coEvery { remote.search("c1", "due", null, SEARCH_PAGE_SIZE) } returns (1..SEARCH_PAGE_SIZE).map { row(it) }

        val page = (repository.search("c1", "due", null) as SearchOutcome.Loaded).page

        assertEquals(SearchCursor("2026-11-14T16:56:30.123456+00:00", "m30"), page.next)
    }

    @Test
    fun `a connection problem is reported`() = runTest {
        coEvery { remote.search(any(), any(), any(), any()) } throws IOException("offline")

        assertEquals(SearchOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.search("c1", "due", null))
    }
}
