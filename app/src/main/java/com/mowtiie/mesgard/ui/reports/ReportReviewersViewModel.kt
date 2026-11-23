package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

internal data class ReportReviewersUiState(
    val candidates: List<Contact> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
) {
    /** With nobody chosen the organization admins review, so the person is told who will. */
    val adminsReview: Boolean
        get() = selectedIds.isEmpty()

    val showEmptyMessage: Boolean
        get() = !isLoading && candidates.isEmpty()
}

/** Choosing who reviews the reports of the organization. Only administrators and staff can be chosen. */
@HiltViewModel
internal class ReportReviewersViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: ReportReviewRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private var organizationId: String? = null

    val uiState: StateFlow<ReportReviewersUiState> = combine(local, contactRepository.observeContacts()) {
            state,
            roster,
        ->
        ReportReviewersUiState(
            candidates = roster.filter { it.role == OrgRole.ORG_ADMIN || it.role == OrgRole.STAFF },
            selectedIds = state.selectedIds,
            isLoading = state.isLoading,
            isSaving = state.isSaving,
            isSaved = state.isSaved,
            error = state.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), ReportReviewersUiState())

    init {
        onRefresh()
    }

    fun onRefresh() {
        local.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            contactRepository.refreshIfStale()
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.reviewers(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            local.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        selectedIds = result.value.map { reviewer -> reviewer.userId }.toSet(),
                        isLoading = false,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onToggled(userId: String) = local.update {
        it.copy(selectedIds = if (userId in it.selectedIds) it.selectedIds - userId else it.selectedIds + userId)
    }

    fun onSaveClicked() {
        val orgId = organizationId ?: return
        local.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.setReviewers(orgId, local.value.selectedIds.toList())
            local.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AdminResult.Failure -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }

    private data class Local(
        val selectedIds: Set<String> = emptySet(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val error: AdminError? = null,
    )
}
