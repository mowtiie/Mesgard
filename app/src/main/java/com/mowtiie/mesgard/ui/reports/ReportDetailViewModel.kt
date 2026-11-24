package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportDecision
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import com.mowtiie.mesgard.ui.navigation.ReportRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val REPORT_DECISION_NOTE_MIN_LENGTH = 3
internal const val REVEAL_REASON_MIN_LENGTH = 10

/** A question the reviewer is asked before something is done. */
internal sealed interface ReportDialog {
    /** Closing a report: [status] is resolved or dismissed. A note is needed. */
    data class Decide(val status: ReportStatus) : ReportDialog

    data object Reveal : ReportDialog

    /** The answer to Reveal. */
    data class Revealed(val reporterName: String) : ReportDialog
}

internal data class ReportDetailUiState(
    val detail: ReportDetail? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val dialog: ReportDialog? = null,
    val canReveal: Boolean = false,
    val error: AdminError? = null,
) {
    /** Only an organization admin can ask who an anonymous reporter is. */
    val showReveal: Boolean
        get() = canReveal && detail?.isAnonymous == true
}

/** One report: the copy of the message, the history, and the decisions a reviewer can make. */
@HiltViewModel
internal class ReportDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReportReviewRepository,
    private val observeAdminOrganization: ObserveAdminOrganization,
) : ViewModel() {
    private val reportId = savedStateHandle.toRoute<ReportRoute>().reportId
    private val mutableUiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isAdmin = observeAdminOrganization.organization.first() != null
            mutableUiState.update { it.copy(canReveal = isAdmin) }
        }
        onRefresh()
    }

    fun onRefresh() {
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.detail(reportId)) {
                is AdminResult.Success -> mutableUiState.update {
                    it.copy(detail = result.value, isLoading = false)
                }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onReviewStarted() = decide(ReportDecision(ReportStatus.IN_REVIEW, note = "", removeMessage = false))

    fun onDialogRequested(dialog: ReportDialog) = mutableUiState.update { it.copy(dialog = dialog) }

    fun onDialogDismissed() = mutableUiState.update { it.copy(dialog = null) }

    /**
     * [followUp] is the removal of the message, the reset of the username or the removal of the picture, whichever the
     * report is about. [blockDays] blocks new pictures for that long after a picture was removed.
     */
    fun onDecisionConfirmed(note: String, followUp: Boolean, blockDays: Int? = null) {
        val status = (mutableUiState.value.dialog as? ReportDialog.Decide)?.status ?: return
        val subject = mutableUiState.value.detail?.subject
        mutableUiState.update { it.copy(dialog = null) }
        decide(
            ReportDecision(
                status = status,
                note = note,
                removeMessage = followUp && subject == ReportSubject.MESSAGE,
                resetUsername = followUp && subject == ReportSubject.USERNAME,
                removePicture = followUp && subject == ReportSubject.PROFILE_PICTURE,
                blockDays = blockDays.takeIf { followUp && subject == ReportSubject.PROFILE_PICTURE },
            ),
        )
    }

    fun onRevealConfirmed(reason: String) {
        mutableUiState.update { it.copy(dialog = null, isWorking = true) }
        viewModelScope.launch {
            when (val result = repository.revealReporter(reportId, reason)) {
                is AdminResult.Success -> {
                    mutableUiState.update {
                        it.copy(isWorking = false, dialog = ReportDialog.Revealed(result.value))
                    }
                    onRefresh()
                }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    private fun decide(decision: ReportDecision) {
        mutableUiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            when (val result = repository.decide(reportId, decision)) {
                is AdminResult.Success -> {
                    mutableUiState.update { it.copy(isWorking = false) }
                    onRefresh()
                }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isWorking = false, error = result.error) }
            }
        }
    }
}
