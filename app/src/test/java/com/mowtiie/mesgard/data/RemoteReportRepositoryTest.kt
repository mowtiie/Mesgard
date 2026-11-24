package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.ReportRemoteSource
import com.mowtiie.mesgard.data.remote.SubmitReportRequest
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.PersonReport
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.conversationEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteReportRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remote = mockk<ReportRemoteSource>()
    private val repository by lazy { RemoteReportRepository(remote, databaseRule.database) }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("c1")))
    }

    private fun report(note: String = "  Unkind  ", conversationId: String = "c1") = MessageReport(
        reportId = "r1",
        conversationId = conversationId,
        messageId = "m1",
        category = ReportCategory.CHILD_SAFETY,
        note = note,
        isAnonymous = true,
    )

    @Test
    fun `a report goes to the organization of the conversation with the category the server knows`() = runTest {
        val sent = slot<SubmitReportRequest>()
        coEvery { remote.submit(capture(sent)) } returns Unit

        val result = repository.reportMessage(report())

        assertEquals(ReportResult.Sent, result)
        assertEquals(
            SubmitReportRequest("r1", TEST_ORG_ID, "child_safety", "message", "m1", null, "Unkind", true),
            sent.captured,
        )
    }

    @Test
    fun `a report about a name goes to the organization of the person with the subject name`() = runTest {
        val sent = slot<SubmitReportRequest>()
        coEvery { remote.submit(capture(sent)) } returns Unit

        val result = repository.reportPerson(
            PersonReport(
                "r2",
                "org1",
                "u2",
                ReportSubject.NAME,
                ReportCategory.IMPERSONATION,
                " Copies a teacher ",
                false,
            ),
        )

        assertEquals(ReportResult.Sent, result)
        assertEquals(
            SubmitReportRequest("r2", "org1", "impersonation", "name", null, "u2", "Copies a teacher", false),
            sent.captured,
        )
    }

    @Test
    fun `details that are only spaces are not sent`() = runTest {
        val sent = slot<SubmitReportRequest>()
        coEvery { remote.submit(capture(sent)) } returns Unit

        repository.reportMessage(report(note = "   "))

        assertEquals(null, sent.captured.note)
    }

    @Test
    fun `a conversation the phone does not know cannot be reported`() = runTest {
        val result = repository.reportMessage(report(conversationId = "unknown"))

        assertEquals(ReportResult.Failure(ReportError.UNKNOWN), result)
        coVerify(exactly = 0) { remote.submit(any()) }
    }

    @Test
    fun `no connection is reported as such`() = runTest {
        coEvery { remote.submit(any()) } throws IOException("offline")

        assertEquals(ReportResult.Failure(ReportError.NETWORK_UNAVAILABLE), repository.reportMessage(report()))
    }

    @Test
    fun `the answer of the server decides the error`() {
        assertEquals(ReportError.NOT_ALLOWED, reportErrorFor(403))
        assertEquals(ReportError.NOT_ALLOWED, reportErrorFor(401))
        assertEquals(ReportError.ALREADY_REPORTED, reportErrorFor(409))
        assertEquals(ReportError.UNKNOWN, reportErrorFor(500))
    }
}
