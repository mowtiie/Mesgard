package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import com.mowtiie.mesgard.ui.reports.ReportFormViewModel
import com.mowtiie.mesgard.ui.reports.ReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ReportMessageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportRepository,
) : ReportFormViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId

    fun onReportRequested(messageId: String) = open(messageId, listOf(ReportSubject.MESSAGE))

    override suspend fun send(state: ReportUiState): ReportResult {
        val category = state.category ?: return ReportResult.Failure(ReportError.UNKNOWN)
        return reports.reportMessage(
            MessageReport(state.reportId, conversationId, state.targetId, category, state.note, state.isAnonymous),
        )
    }
}
