package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.GroupReport
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.ui.navigation.GroupRoute
import com.mowtiie.mesgard.ui.reports.ReportFormViewModel
import com.mowtiie.mesgard.ui.reports.ReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Reporting the name or the picture of a group, from the group screen. */
@HiltViewModel
internal class GroupReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportRepository,
) : ReportFormViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupRoute>().groupId
    private var orgId = ""

    /** The picture can only be reported while the group has one. */
    fun onReportRequested(orgId: String, hasPicture: Boolean) {
        this.orgId = orgId
        open(
            groupId,
            buildList {
                add(ReportSubject.GROUP_NAME)
                if (hasPicture) add(ReportSubject.GROUP_PICTURE)
            },
        )
    }

    override suspend fun send(state: ReportUiState): ReportResult {
        val subject = state.subject
        val category = state.category
        if (subject == null || category == null || orgId.isEmpty()) return ReportResult.Failure(ReportError.UNKNOWN)
        return reports.reportGroup(
            GroupReport(state.reportId, orgId, groupId, subject, category, state.note, state.isAnonymous),
        )
    }
}
