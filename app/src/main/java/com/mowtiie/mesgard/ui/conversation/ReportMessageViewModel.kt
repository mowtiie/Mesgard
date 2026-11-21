package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.ui.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ReportStage { FORM, SENDING, SENT }

internal data class ReportMessageUiState(
    val isOpen: Boolean = false,
    val messageId: String = "",
    /** Chosen when the sheet opens, so sending again after a failure does not create a second report. */
    val reportId: String = "",
    val category: ReportCategory? = null,
    val note: String = "",
    val isAnonymous: Boolean = false,
    val stage: ReportStage = ReportStage.FORM,
    val error: ReportError? = null,
) {
    val canSend: Boolean
        get() = category != null && stage == ReportStage.FORM
}

/** Reporting one message: the person chooses what is wrong, may add details and may stay anonymous to reviewers. */
@HiltViewModel
internal class ReportMessageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.toRoute<ConversationRoute>().conversationId
    private val mutableUiState = MutableStateFlow(ReportMessageUiState())
    val uiState: StateFlow<ReportMessageUiState> = mutableUiState.asStateFlow()

    fun onReportRequested(messageId: String) {
        mutableUiState.value = ReportMessageUiState(
            isOpen = true,
            messageId = messageId,
            reportId = UUID.randomUUID().toString(),
        )
    }

    fun onCategoryChanged(category: ReportCategory) = mutableUiState.update { it.copy(category = category) }

    fun onNoteChanged(note: String) = mutableUiState.update { it.copy(note = note.take(REPORT_NOTE_MAX_LENGTH)) }

    fun onAnonymousChanged(isAnonymous: Boolean) = mutableUiState.update { it.copy(isAnonymous = isAnonymous) }

    fun onDismissed() {
        mutableUiState.value = ReportMessageUiState()
    }

    fun onSendClicked() {
        val report = mutableUiState.value.toReport(conversationId) ?: return
        mutableUiState.update { it.copy(stage = ReportStage.SENDING, error = null) }
        viewModelScope.launch {
            val result = reports.reportMessage(report)
            mutableUiState.update { it.after(result) }
        }
    }
}

private fun ReportMessageUiState.toReport(conversationId: String): MessageReport? =
    category?.takeIf { canSend }?.let { MessageReport(reportId, conversationId, messageId, it, note, isAnonymous) }

/** A sheet closed while the report was on its way stays closed. A report made before counts as sent. */
private fun ReportMessageUiState.after(result: ReportResult): ReportMessageUiState = when {
    !isOpen -> this
    result is ReportResult.Sent -> copy(stage = ReportStage.SENT)
    result is ReportResult.Failure && result.error == ReportError.ALREADY_REPORTED ->
        copy(stage = ReportStage.SENT, error = result.error)
    result is ReportResult.Failure -> copy(stage = ReportStage.FORM, error = result.error)
    else -> this
}
