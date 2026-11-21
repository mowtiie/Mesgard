package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportMessageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ReportRepository>()
    private val viewModel by lazy {
        ReportMessageViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository)
    }

    @Test
    fun `a report cannot be sent before the person says what is wrong`() = runTest {
        viewModel.onReportRequested("m1")

        assertFalse(viewModel.uiState.value.canSend)
        viewModel.onSendClicked()

        coVerify(exactly = 0) { repository.reportMessage(any()) }
    }

    @Test
    fun `sending a report carries the choices of the person`() = runTest {
        val sent = slot<MessageReport>()
        coEvery { repository.reportMessage(capture(sent)) } returns ReportResult.Sent
        viewModel.onReportRequested("m1")
        viewModel.onCategoryChanged(ReportCategory.HARASSMENT)
        viewModel.onNoteChanged("Messages after class")
        viewModel.onAnonymousChanged(true)

        viewModel.onSendClicked()

        assertEquals(
            MessageReport(
                reportId = viewModel.uiState.value.reportId,
                conversationId = "c1",
                messageId = "m1",
                category = ReportCategory.HARASSMENT,
                note = "Messages after class",
                isAnonymous = true,
            ),
            sent.captured,
        )
        assertEquals(ReportStage.SENT, viewModel.uiState.value.stage)
    }

    @Test
    fun `a failed report can be sent again under the same report id`() = runTest {
        coEvery { repository.reportMessage(any()) } returns ReportResult.Failure(ReportError.NETWORK_UNAVAILABLE)
        viewModel.onReportRequested("m1")
        viewModel.onCategoryChanged(ReportCategory.OTHER)
        val reportId = viewModel.uiState.value.reportId

        viewModel.onSendClicked()

        assertEquals(ReportStage.FORM, viewModel.uiState.value.stage)
        assertEquals(ReportError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
        coEvery { repository.reportMessage(any()) } returns ReportResult.Sent
        viewModel.onSendClicked()
        assertEquals(ReportStage.SENT, viewModel.uiState.value.stage)
        coVerify(exactly = 2) { repository.reportMessage(match { it.reportId == reportId }) }
    }

    @Test
    fun `a message that was already reported counts as sent`() = runTest {
        coEvery { repository.reportMessage(any()) } returns ReportResult.Failure(ReportError.ALREADY_REPORTED)
        viewModel.onReportRequested("m1")
        viewModel.onCategoryChanged(ReportCategory.BULLYING)

        viewModel.onSendClicked()

        assertEquals(ReportStage.SENT, viewModel.uiState.value.stage)
        assertEquals(ReportError.ALREADY_REPORTED, viewModel.uiState.value.error)
    }

    @Test
    fun `details are cut at the length the server accepts`() = runTest {
        viewModel.onReportRequested("m1")

        viewModel.onNoteChanged("a".repeat(REPORT_NOTE_MAX_LENGTH + 50))

        assertEquals(REPORT_NOTE_MAX_LENGTH, viewModel.uiState.value.note.length)
    }

    @Test
    fun `each time the sheet opens it is a new report`() = runTest {
        viewModel.onReportRequested("m1")
        val first = viewModel.uiState.value.reportId
        viewModel.onCategoryChanged(ReportCategory.OTHER)
        viewModel.onDismissed()

        assertFalse(viewModel.uiState.value.isOpen)
        viewModel.onReportRequested("m2")

        assertTrue(viewModel.uiState.value.isOpen)
        assertNotEquals(first, viewModel.uiState.value.reportId)
        assertEquals(null, viewModel.uiState.value.category)
    }
}
