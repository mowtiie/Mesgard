package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AccessedMessageDto
import com.mowtiie.mesgard.data.remote.GroupAccessRemoteSource
import com.mowtiie.mesgard.data.remote.OpeningDto
import com.mowtiie.mesgard.domain.model.AdminResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupAccessRepositoryTest {
    private val remote = mockk<GroupAccessRemoteSource>()
    private val repository = RemoteGroupAccessRepository(remote)

    @Test
    fun `opens a group with the trimmed reason`() = runTest {
        coEvery { remote.open("g1", "Report 114 names a student") } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.open("g1", "  Report 114 names a student \n"))
        coVerify { remote.open("g1", "Report 114 names a student") }
    }

    @Test
    fun `reads a page and starts the next one after the last message exactly`() = runTest {
        val first = AccessedMessageDto("m2", "Ana", "Hello", "2026-11-22T10:00:00.123456+00:00")
        val deleted =
            AccessedMessageDto("m1", null, null, "2026-11-22T09:00:00+00:00", deletedAt = "2026-11-22T09:30:00+00:00")
        coEvery { remote.messages("g1", null, null, 50) } returns listOf(first, deleted)
        coEvery { remote.messages("g1", "2026-11-22T09:00:00+00:00", "m1", 50) } returns emptyList()

        val page = (repository.messages("g1", null, 50) as AdminResult.Success).value

        assertEquals(listOf("Hello", null), page.map { it.body })
        assertEquals(listOf(false, true), page.map { it.isDeleted })
        assertEquals(AdminResult.Success(emptyList<Any>()), repository.messages("g1", page.last(), 50))
    }

    @Test
    fun `reads when the group was opened as times`() = runTest {
        coEvery { remote.openings("g1") } returns listOf(OpeningDto("2026-11-22T10:00:00+00:00"))

        val openings = (repository.openings("g1") as AdminResult.Success).value

        assertTrue(openings.single() > 0)
    }
}
