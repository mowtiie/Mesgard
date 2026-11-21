package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.LegalHoldDto
import com.mowtiie.mesgard.data.remote.LegalHoldRemoteSource
import com.mowtiie.mesgard.data.remote.PlaceHoldRequest
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.LegalHoldScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteLegalHoldRepositoryTest {
    private val remote = mockk<LegalHoldRemoteSource>()
    private val repository = RemoteLegalHoldRepository(remote)

    @Test
    fun `holds are listed with what they protect`() = runTest {
        coEvery { remote.activeHolds("org") } returns listOf(
            LegalHoldDto("h1", "person", "Student Two", null, "A court order", "Admin", "2026-11-20T08:00:00+00:00"),
            LegalHoldDto("h2", "group", null, "Section A", "An inquiry", "Admin", "2026-11-20T09:00:00+00:00"),
            LegalHoldDto("h3", "organization", null, null, "An inquiry", "Admin", "2026-11-20T10:00:00+00:00"),
        )

        val holds = (repository.activeHolds("org") as AdminResult.Success).value

        assertEquals(
            listOf(LegalHoldScope.PERSON, LegalHoldScope.GROUP, LegalHoldScope.ORGANIZATION),
            holds.map { it.scope },
        )
        assertEquals("Student Two", holds[0].personName)
        assertEquals("Section A", holds[1].groupName)
    }

    @Test
    fun `a hold on a person sends the person and no group`() = runTest {
        coEvery { remote.place(any()) } returns Unit

        repository.place("org", HoldTarget.Person("u1", "Student Two"), "  A court order was received ")

        coVerify { remote.place(PlaceHoldRequest("org", "A court order was received", "u1", null)) }
    }

    @Test
    fun `a hold on a group sends the group and no person`() = runTest {
        coEvery { remote.place(any()) } returns Unit

        repository.place("org", HoldTarget.Group("g1", "Section A"), "An inquiry covers the class")

        coVerify { remote.place(PlaceHoldRequest("org", "An inquiry covers the class", null, "g1")) }
    }

    @Test
    fun `a hold on the organization names nobody`() = runTest {
        coEvery { remote.place(any()) } returns Unit

        repository.place("org", HoldTarget.Organization, "An inquiry covers the whole school")

        coVerify { remote.place(PlaceHoldRequest("org", "An inquiry covers the whole school", null, null)) }
    }

    @Test
    fun `releasing sends a trimmed note`() = runTest {
        coEvery { remote.release("h1", any()) } returns Unit

        repository.release("h1", "  The order was withdrawn ")

        coVerify { remote.release("h1", "The order was withdrawn") }
    }

    @Test
    fun `no connection is reported as such`() = runTest {
        coEvery { remote.activeHolds("org") } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.activeHolds("org"))
    }
}
