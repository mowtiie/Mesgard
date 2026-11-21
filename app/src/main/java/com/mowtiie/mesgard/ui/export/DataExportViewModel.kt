package com.mowtiie.mesgard.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.repository.DataExportRepository
import com.mowtiie.mesgard.domain.repository.ExportFile
import com.mowtiie.mesgard.domain.repository.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DataExportUiState(
    val isExporting: Boolean = false,
    val file: ExportFile? = null,
    val error: AdminError? = null,
)

/** Creates the export of one person for the organization the signed in person administers. */
@HiltViewModel
internal class DataExportViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: DataExportRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DataExportUiState())
    val uiState: StateFlow<DataExportUiState> = mutableUiState.asStateFlow()

    fun onExportClicked(userId: String, personName: String) {
        mutableUiState.value = DataExportUiState(isExporting = true)
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId
            val result = orgId?.let { repository.export(it, userId, personName) }
                ?: ExportResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.value = when (result) {
                is ExportResult.Ready -> DataExportUiState(file = result.file)
                is ExportResult.Failure -> DataExportUiState(error = result.error)
            }
        }
    }

    /** The dialog was closed, so the next one starts clean. */
    fun onDialogClosed() = mutableUiState.update { DataExportUiState() }
}
