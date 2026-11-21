package com.mowtiie.mesgard.ui.retention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ONE_YEAR_DAYS = 365
private const val TWO_YEARS_DAYS = 730
private const val FIVE_YEARS_DAYS = 1825

/** Keep without a limit, or for a year, two or five. */
internal val RetentionPresets: List<Int?> = listOf(null, ONE_YEAR_DAYS, TWO_YEARS_DAYS, FIVE_YEARS_DAYS)

private fun optionsFor(current: Int?): List<Int?> =
    if (current in RetentionPresets) RetentionPresets else RetentionPresets + current

internal data class RetentionUiState(
    /** The limit in days, or null for no limit. */
    val selectedDays: Int? = null,
    /** The presets, and the current limit when the server was given one that is not a preset. */
    val options: List<Int?> = RetentionPresets,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
)

/** An organization admin chooses how long messages are kept. */
@HiltViewModel
internal class RetentionViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: RetentionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RetentionUiState())
    val uiState: StateFlow<RetentionUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null

    init {
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.retentionDays(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        selectedDays = result.value,
                        options = optionsFor(result.value),
                        isLoading = false,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onSelected(days: Int?) = mutableUiState.update { it.copy(selectedDays = days) }

    fun onSaveClicked() {
        val orgId = organizationId ?: return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.setRetentionDays(orgId, mutableUiState.value.selectedDays)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AdminResult.Failure -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
