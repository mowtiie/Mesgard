package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ReportStage { FORM, SENDING, SENT }

internal data class ReportUiState(
    val isOpen: Boolean = false,
    /** The message or the person being reported. */
    val targetId: String = "",
    /** Chosen when the sheet opens, so sending again after a failure does not create a second report. */
    val reportId: String = "",
    /** What can be reported about the target. With one choice, the person is not asked. */
    val subjects: List<ReportSubject> = emptyList(),
    val subject: ReportSubject? = null,
    val category: ReportCategory? = null,
    val note: String = "",
    val isAnonymous: Boolean = false,
    val stage: ReportStage = ReportStage.FORM,
    val error: ReportError? = null,
) {
    val canSend: Boolean
        get() = subject != null && category != null && stage == ReportStage.FORM

    val asksForSubject: Boolean
        get() = subjects.size > 1

    val categories: List<ReportCategory>
        get() = subject?.let(ReportCategory::forSubject).orEmpty()
}

/** Reporting something: the person chooses what is wrong, may add details and may stay anonymous to reviewers. */
internal abstract class ReportFormViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = mutableUiState.asStateFlow()

    protected fun open(targetId: String, subjects: List<ReportSubject>) {
        mutableUiState.value = ReportUiState(
            isOpen = true,
            targetId = targetId,
            reportId = UUID.randomUUID().toString(),
            subjects = subjects,
            subject = subjects.singleOrNull(),
        )
    }

    fun onSubjectChanged(subject: ReportSubject) = mutableUiState.update { it.copy(subject = subject, category = null) }

    fun onCategoryChanged(category: ReportCategory) = mutableUiState.update { it.copy(category = category) }

    fun onNoteChanged(note: String) = mutableUiState.update { it.copy(note = note.take(REPORT_NOTE_MAX_LENGTH)) }

    fun onAnonymousChanged(isAnonymous: Boolean) = mutableUiState.update { it.copy(isAnonymous = isAnonymous) }

    fun onDismissed() {
        mutableUiState.value = ReportUiState()
    }

    fun onSendClicked() {
        val state = mutableUiState.value
        if (!state.canSend) return
        mutableUiState.update { it.copy(stage = ReportStage.SENDING, error = null) }
        viewModelScope.launch {
            val result = send(state)
            mutableUiState.update { it.after(result) }
        }
    }

    protected abstract suspend fun send(state: ReportUiState): ReportResult
}

/** A sheet closed while the report was on its way stays closed. A report made before counts as sent. */
private fun ReportUiState.after(result: ReportResult): ReportUiState = when {
    !isOpen -> this
    result is ReportResult.Sent -> copy(stage = ReportStage.SENT)
    result is ReportResult.Failure && result.error == ReportError.ALREADY_REPORTED ->
        copy(stage = ReportStage.SENT, error = result.error)
    result is ReportResult.Failure -> copy(stage = ReportStage.FORM, error = result.error)
    else -> this
}
