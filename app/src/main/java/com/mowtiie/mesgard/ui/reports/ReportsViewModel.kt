package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportCursor
import com.mowtiie.mesgard.domain.model.ReportPage
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSummary
import com.mowtiie.mesgard.domain.reports.FindReviewOrganization
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ReportsUiState(
    val status: ReportStatus = ReportStatus.OPEN,
    val reports: List<ReportSummary> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val error: AdminError? = null,
) {
    val showEmptyMessage: Boolean
        get() = reports.isEmpty() && !isLoading && error == null
}

/** The reports of the organization the person reviews, one status at a time, newest first. */
@HiltViewModel
internal class ReportsViewModel @Inject constructor(
    private val findReviewOrganization: FindReviewOrganization,
    private val repository: ReportReviewRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null
    private var next: ReportCursor? = null

    init {
        onRefresh()
    }

    fun onRefresh() = load(before = null)

    fun onLoadMore() {
        val cursor = next ?: return
        load(before = cursor)
    }

    fun onStatusSelected(status: ReportStatus) {
        if (status == mutableUiState.value.status) return
        mutableUiState.update { it.copy(status = status, reports = emptyList(), hasMore = false) }
        load(before = null)
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    private fun load(before: ReportCursor?) {
        if (mutableUiState.value.isLoading && before != null) return
        val requested = mutableUiState.value.status
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val orgId = organizationId ?: findReviewOrganization()?.also { organizationId = it }
            val result = orgId?.let { repository.list(it, requested, before) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            // A list for a status the person has already left is dropped.
            if (mutableUiState.value.status != requested) return@launch
            if (result is AdminResult.Success) next = result.value.next
            mutableUiState.update { it.after(result, before) }
        }
    }
}

private fun ReportsUiState.after(result: AdminResult<ReportPage>, before: ReportCursor?) = when (result) {
    is AdminResult.Success -> copy(
        reports = if (before == null) result.value.reports else reports + result.value.reports,
        hasMore = result.value.next != null,
        isLoading = false,
    )
    is AdminResult.Failure -> copy(isLoading = false, error = result.error)
}
