package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.EvidenceDto
import com.mowtiie.mesgard.data.remote.EvidenceFileDto
import com.mowtiie.mesgard.data.remote.EvidenceLineDto
import com.mowtiie.mesgard.data.remote.ReportDecisionRequest
import com.mowtiie.mesgard.data.remote.ReportDetailDto
import com.mowtiie.mesgard.data.remote.ReportEventDto
import com.mowtiie.mesgard.data.remote.ReportReviewRemoteSource
import com.mowtiie.mesgard.data.remote.ReportRowDto
import com.mowtiie.mesgard.data.remote.RevealedReporterDto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportCursor
import com.mowtiie.mesgard.domain.model.ReportDecision
import com.mowtiie.mesgard.domain.model.ReportEventType
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.repository.REPORT_PAGE_SIZE
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteReportReviewRepositoryTest {
    private val remote = mockk<ReportReviewRemoteSource>()
    private val repository = RemoteReportReviewRepository(remote)

    private fun row(id: String, anonymous: Boolean = false) = ReportRowDto(
        reportId = id,
        category = "harassment",
        status = "open",
        subjectName = "Student Two",
        anonymous = anonymous,
        reporterName = if (anonymous) null else "Student One",
        createdAt = "2026-11-20T08:00:00+00:00",
    )

    @Test
    fun `a full page offers the next one and keeps the anonymity of the reporter`() = runTest {
        val rows = List(REPORT_PAGE_SIZE) { row("r$it", anonymous = it == 0) }
        coEvery { remote.list("org", "open", null, REPORT_PAGE_SIZE) } returns rows

        val page = (repository.list("org", ReportStatus.OPEN, null) as AdminResult.Success).value

        assertEquals(ReportCursor("2026-11-20T08:00:00+00:00", "r${REPORT_PAGE_SIZE - 1}"), page.next)
        assertNull(page.reports.first().reporterName)
        assertTrue(page.reports.first().isAnonymous)
        assertEquals(ReportCategory.HARASSMENT, page.reports.first().category)
    }

    @Test
    fun `a short page is the last one`() = runTest {
        coEvery { remote.list("org", "resolved", null, REPORT_PAGE_SIZE) } returns listOf(row("r1"))

        val page = (repository.list("org", ReportStatus.RESOLVED, null) as AdminResult.Success).value

        assertNull(page.next)
    }

    @Test
    fun `a case keeps the copy of the message, the files and the history`() = runTest {
        coEvery { remote.detail("r1") } returns ReportDetailDto(
            id = "r1",
            category = "child_safety",
            status = "in_review",
            anonymous = true,
            subjectName = "Staff One",
            createdAt = "2026-11-20T08:00:00+00:00",
            evidence = EvidenceDto(
                conversationTitle = null,
                senderName = "Staff One",
                body = "Of course.",
                sentAt = "2026-11-20T07:00:00+00:00",
                context = listOf(EvidenceLineDto("Student One", "Hello", "2026-11-20T06:59:00+00:00")),
            ),
            files = listOf(EvidenceFileDto("photo.png", "image/png", 2048)),
            events = listOf(ReportEventDto("reporter_revealed", "Checking", null, "2026-11-20T09:00:00+00:00")),
        )

        val detail = (repository.detail("r1") as AdminResult.Success).value

        assertEquals(ReportCategory.CHILD_SAFETY, detail.category)
        assertEquals(ReportStatus.IN_REVIEW, detail.status)
        assertEquals("Of course.", detail.evidence?.message?.body)
        assertEquals("Hello", detail.evidence?.earlier?.single()?.body)
        assertEquals(2048L, detail.files.single().sizeBytes)
        assertEquals(ReportEventType.REPORTER_REVEALED, detail.events.single().type)
    }

    @Test
    fun `a decision sends the status and a trimmed note`() = runTest {
        val sent = slot<ReportDecisionRequest>()
        coEvery { remote.decide(capture(sent)) } returns Unit

        repository.decide("r1", ReportDecision(ReportStatus.RESOLVED, "  Spoke with both  ", removeMessage = true))

        assertEquals(ReportDecisionRequest("r1", "resolved", "Spoke with both", true), sent.captured)
    }

    @Test
    fun `starting a review sends no note`() = runTest {
        val sent = slot<ReportDecisionRequest>()
        coEvery { remote.decide(capture(sent)) } returns Unit

        repository.decide("r1", ReportDecision(ReportStatus.IN_REVIEW, note = "", removeMessage = false))

        assertNull(sent.captured.note)
    }

    @Test
    fun `revealing a reporter returns the name`() = runTest {
        coEvery { remote.revealReporter("r1", "Checking a claim") } returns RevealedReporterDto("Student One")

        assertEquals(AdminResult.Success("Student One"), repository.revealReporter("r1", "  Checking a claim "))
    }

    @Test
    fun `reviewers are saved by their ids`() = runTest {
        coEvery { remote.setReviewers("org", listOf("u1", "u2")) } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.setReviewers("org", listOf("u1", "u2")))
        coVerify { remote.setReviewers("org", listOf("u1", "u2")) }
    }

    @Test
    fun `no connection is reported as such`() = runTest {
        coEvery { remote.canReview("org") } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.canReview("org"))
    }
}
