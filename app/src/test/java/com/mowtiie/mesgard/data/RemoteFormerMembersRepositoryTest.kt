package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.FormerMemberDto
import com.mowtiie.mesgard.data.remote.FormerMembersRemoteSource
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import com.mowtiie.mesgard.domain.repository.FORMER_MEMBERS_PAGE_SIZE
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ORG = "org-1"

@RunWith(RobolectricTestRunner::class)
class RemoteFormerMembersRepositoryTest {
    private val remote = mockk<FormerMembersRemoteSource>()
    private val repository = RemoteFormerMembersRepository(remote)

    private fun row(number: Int, erased: Boolean = false, canErase: Boolean = true) = FormerMemberDto(
        userId = "u$number",
        displayName = "Person $number",
        removedAt = "2026-11-14T16:56:%02d.123456+00:00".format(number),
        isErased = erased,
        canErase = canErase,
    )

    @Test
    fun `reads the name, the time and whether the person can still be erased`() = runTest {
        coEvery { remote.fetchPage(ORG, null, FORMER_MEMBERS_PAGE_SIZE) } returns
            listOf(row(5), row(4, erased = true, canErase = false))

        val page = (repository.fetchPage(ORG, null) as AdminResult.Success).value

        assertEquals(listOf("u5", "u4"), page.members.map { it.userId })
        assertEquals(1_794_675_365_123L, page.members.first().removedAtEpochMillis)
        assertTrue(page.members.first().canErase)
        assertTrue(page.members.last().isErased)
        assertFalse(page.members.last().canErase)
        assertNull(page.next)
    }

    @Test
    fun `a full page points to the next one after its last person`() = runTest {
        coEvery { remote.fetchPage(ORG, null, FORMER_MEMBERS_PAGE_SIZE) } returns
            (1..FORMER_MEMBERS_PAGE_SIZE).map { row(it) }

        val page = (repository.fetchPage(ORG, null) as AdminResult.Success).value

        assertEquals(FormerMemberCursor("2026-11-14T16:56:50.123456+00:00", "u50"), page.next)
    }

    @Test
    fun `asks for the page after the cursor it was given`() = runTest {
        val cursor = FormerMemberCursor("2026-11-14T16:56:01+00:00", "u1")
        coEvery { remote.fetchPage(ORG, cursor, FORMER_MEMBERS_PAGE_SIZE) } returns listOf(row(0))

        val page = (repository.fetchPage(ORG, cursor) as AdminResult.Success).value

        assertEquals(listOf("u0"), page.members.map { it.userId })
    }

    @Test
    fun `a connection problem is reported as unavailable`() = runTest {
        coEvery { remote.fetchPage(any(), any(), any()) } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.fetchPage(ORG, null))
    }
}
