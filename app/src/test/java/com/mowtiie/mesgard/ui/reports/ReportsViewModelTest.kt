package com.mowtiie.mesgard.ui.reports

import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportCursor
import com.mowtiie.mesgard.domain.model.ReportPage
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.model.ReportSummary
import com.mowtiie.mesgard.domain.reports.FindReviewOrganization
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val findReviewOrganization = mockk<FindReviewOrganization>()
    private val repository = mockk<ReportReviewRepository>()

    private fun summary(id: String) = ReportSummary(
        id = id,
        category = ReportCategory.BULLYING,
        subject = ReportSubject.MESSAGE,
        status = ReportStatus.OPEN,
        subjectName = "Student Two",
        isAnonymous = false,
        reporterName = "Student One",
        createdAtEpochMillis = 0L,
    )

    private fun viewModel() = ReportsViewModel(findReviewOrganization, repository)

    @Test
    fun `the open reports of the organization are listed first`() = runTest {
        coEvery { findReviewOrganization() } returns "org"
        coEvery { repository.list("org", ReportStatus.OPEN, null) } returns
            AdminResult.Success(ReportPage(listOf(summary("r1")), next = null))

        val state = viewModel().uiState.value

        assertEquals(listOf("r1"), state.reports.map { it.id })
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
    }

    @Test
    fun `a person who reviews nothing is told so`() = runTest {
        coEvery { findReviewOrganization() } returns null

        val state = viewModel().uiState.value

        assertEquals(AdminError.NOT_ALLOWED, state.error)
        assertFalse(state.showEmptyMessage)
    }

    @Test
    fun `choosing another status lists that status from the start`() = runTest {
        coEvery { findReviewOrganization() } returns "org"
        coEvery { repository.list("org", ReportStatus.OPEN, null) } returns
            AdminResult.Success(ReportPage(emptyList(), null))
        coEvery { repository.list("org", ReportStatus.RESOLVED, null) } returns
            AdminResult.Success(ReportPage(listOf(summary("r9")), null))
        val viewModel = viewModel()

        viewModel.onStatusSelected(ReportStatus.RESOLVED)

        assertEquals(ReportStatus.RESOLVED, viewModel.uiState.value.status)
        assertEquals(listOf("r9"), viewModel.uiState.value.reports.map { it.id })
    }

    @Test
    fun `more reports are added after the ones shown`() = runTest {
        val cursor = ReportCursor("2026-11-20T08:00:00+00:00", "r1")
        coEvery { findReviewOrganization() } returns "org"
        coEvery { repository.list("org", ReportStatus.OPEN, null) } returns
            AdminResult.Success(ReportPage(listOf(summary("r1")), next = cursor))
        coEvery { repository.list("org", ReportStatus.OPEN, cursor) } returns
            AdminResult.Success(ReportPage(listOf(summary("r2")), next = null))
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.onLoadMore()

        assertEquals(listOf("r1", "r2"), viewModel.uiState.value.reports.map { it.id })
        assertFalse(viewModel.uiState.value.hasMore)
        coVerify(exactly = 1) { findReviewOrganization() }
    }

    @Test
    fun `an empty list shows the empty message`() = runTest {
        coEvery { findReviewOrganization() } returns "org"
        coEvery { repository.list("org", ReportStatus.OPEN, null) } returns
            AdminResult.Success(ReportPage(emptyList(), null))

        assertTrue(viewModel().uiState.value.showEmptyMessage)
    }
}
