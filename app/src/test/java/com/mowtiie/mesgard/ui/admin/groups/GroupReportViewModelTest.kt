package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.GroupReport
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reports = mockk<ReportRepository>()
    private val viewModel = GroupReportViewModel(SavedStateHandle(mapOf("groupId" to "g1")), reports)

    @Test
    fun `a group without a picture can only be reported for its name, which is not asked`() {
        viewModel.onReportRequested("org1", hasPicture = false)

        val state = viewModel.uiState.value
        assertEquals(listOf(ReportSubject.GROUP_NAME), state.subjects)
        assertEquals(ReportSubject.GROUP_NAME, state.subject)
    }

    @Test
    fun `a group with a picture asks whether the name or the picture is reported`() {
        viewModel.onReportRequested("org1", hasPicture = true)

        val state = viewModel.uiState.value
        assertEquals(listOf(ReportSubject.GROUP_NAME, ReportSubject.GROUP_PICTURE), state.subjects)
        assertEquals(null, state.subject)
    }

    @Test
    fun `the report names the organization, the group, what is reported and why`() {
        val sent = slot<GroupReport>()
        coEvery { reports.reportGroup(capture(sent)) } returns ReportResult.Sent
        viewModel.onReportRequested("org1", hasPicture = true)

        viewModel.onSubjectChanged(ReportSubject.GROUP_PICTURE)
        viewModel.onCategoryChanged(ReportCategory.OFFENSIVE_CONTENT)
        viewModel.onNoteChanged("Not suitable")
        viewModel.onSendClicked()

        val state = viewModel.uiState.value
        assertEquals(
            GroupReport(
                state.reportId,
                "org1",
                "g1",
                ReportSubject.GROUP_PICTURE,
                ReportCategory.OFFENSIVE_CONTENT,
                "Not suitable",
                false,
            ),
            sent.captured,
        )
    }
}
