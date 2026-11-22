package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.DocumentStore
import com.mowtiie.mesgard.domain.admin.GROUP_TEMPLATE
import com.mowtiie.mesgard.domain.admin.GroupFileProblem
import com.mowtiie.mesgard.domain.admin.GroupImportRow
import com.mowtiie.mesgard.domain.admin.GroupParse
import com.mowtiie.mesgard.domain.admin.MAX_ROSTER_BYTES
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.admin.parseGroupFile
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupImportResult
import com.mowtiie.mesgard.domain.repository.GroupImportRepository
import com.mowtiie.mesgard.ui.admin.ImportMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ImportGroupsStage {
    START,
    PREVIEW,
    IMPORTING,
    DONE,
}

internal data class ImportGroupsUiState(
    val stage: ImportGroupsStage = ImportGroupsStage.START,
    val rows: List<GroupImportRow> = emptyList(),
    val fileProblem: GroupFileProblem? = null,
    val result: GroupImportResult? = null,
    val error: AdminError? = null,
    val message: ImportMessage? = null,
) {
    val readyRows: List<GroupImportRow>
        get() = rows.filter { it.problem == null }

    val problemRows: List<GroupImportRow>
        get() = rows.filter { it.problem != null }

    val groupCount: Int
        get() = readyRows.map { it.parent.orEmpty().lowercase() to it.group.lowercase() }.toSet().size

    val peopleCount: Int
        get() = readyRows.count { it.email != null }
}

/** An organization admin creates groups, and puts people in them, from a file. */
@HiltViewModel
internal class ImportGroupsViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: GroupImportRepository,
    private val documentStore: DocumentStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ImportGroupsUiState())
    val uiState: StateFlow<ImportGroupsUiState> = mutableUiState.asStateFlow()

    fun onFileChosen(uri: String) {
        viewModelScope.launch {
            val text = documentStore.readText(uri, MAX_ROSTER_BYTES)
            val parse = if (text == null) GroupParse.Rejected(GroupFileProblem.UNREADABLE) else parseGroupFile(text)
            mutableUiState.update {
                when (parse) {
                    is GroupParse.Parsed -> it.copy(
                        stage = ImportGroupsStage.PREVIEW,
                        rows = parse.rows,
                        fileProblem = null,
                    )
                    is GroupParse.Rejected -> it.copy(stage = ImportGroupsStage.START, fileProblem = parse.problem)
                }
            }
        }
    }

    fun onChooseAnotherFile() = mutableUiState.update { ImportGroupsUiState() }

    fun onImportClicked() {
        val ready = mutableUiState.value.readyRows
        if (mutableUiState.value.stage != ImportGroupsStage.PREVIEW || ready.isEmpty()) return
        mutableUiState.update { it.copy(stage = ImportGroupsStage.IMPORTING) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val result =
                organizationId?.let { repository.import(it, ready) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(stage = ImportGroupsStage.DONE, result = result.value)
                    is AdminResult.Failure -> it.copy(stage = ImportGroupsStage.PREVIEW, error = result.error)
                }
            }
        }
    }

    fun onTemplateDestinationChosen(uri: String) {
        viewModelScope.launch {
            val saved = documentStore.writeText(uri, GROUP_TEMPLATE)
            mutableUiState.update { it.copy(message = if (saved) ImportMessage.SAVED else ImportMessage.SAVE_FAILED) }
        }
    }

    fun onMessageShown() = mutableUiState.update { it.copy(message = null) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
