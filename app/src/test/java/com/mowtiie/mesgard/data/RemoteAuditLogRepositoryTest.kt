package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AuditLogRemoteSource
import com.mowtiie.mesgard.data.remote.AuditRowDto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditCursor
import com.mowtiie.mesgard.domain.repository.AUDIT_PAGE_SIZE
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteAuditLogRepositoryTest {
    private val remote = mockk<AuditLogRemoteSource>()
    private val repository = RemoteAuditLogRepository(remote)

    private fun row(number: Int, details: Map<String, String> = emptyMap()) = AuditRowDto(
        id = "id-$number",
        actorId = "admin",
        action = "member.added",
        targetId = "u$number",
        metadata = JsonObject(details.mapValues { JsonPrimitive(it.value) }),
        createdAt = "2026-11-14T16:56:%02d.123456+00:00".format(number),
    )

    @Test
    fun `reads the entries with their details and time`() = runTest {
        coEvery { remote.fetchPage(ORG, null, AUDIT_PAGE_SIZE) } returns listOf(row(5, mapOf("role" to "staff")))

        val page = (repository.fetchPage(ORG, null) as AdminResult.Success).value

        val entry = page.entries.single()
        assertEquals("id-5", entry.id)
        assertEquals(mapOf("role" to "staff"), entry.details)
        assertEquals("u5", entry.targetId)
        assertEquals(1_794_675_365_123L, entry.createdAtEpochMillis)
        assertNull(page.next)
    }

    @Test
    fun `a full page points to the next one after its last entry`() = runTest {
        val rows = (1..AUDIT_PAGE_SIZE).map { row(it) }
        coEvery { remote.fetchPage(ORG, null, AUDIT_PAGE_SIZE) } returns rows

        val page = (repository.fetchPage(ORG, null) as AdminResult.Success).value

        assertEquals(AuditCursor("2026-11-14T16:56:50.123456+00:00", "id-50"), page.next)
    }

    @Test
    fun `asks for the page after a cursor`() = runTest {
        val cursor = AuditCursor("2026-11-14T16:56:50.123456+00:00", "id-50")
        coEvery { remote.fetchPage(ORG, cursor, AUDIT_PAGE_SIZE) } returns emptyList()

        repository.fetchPage(ORG, cursor)

        coVerify { remote.fetchPage(ORG, cursor, AUDIT_PAGE_SIZE) }
    }

    @Test
    fun `an unreachable server is a network problem`() = runTest {
        coEvery { remote.fetchPage(ORG, null, AUDIT_PAGE_SIZE) } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.fetchPage(ORG, null))
    }

    private companion object {
        const val ORG = "org"
    }
}
