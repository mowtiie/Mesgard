package com.mowtiie.mesgard.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.DocumentStore
import com.mowtiie.mesgard.domain.admin.IssuedCredential
import com.mowtiie.mesgard.domain.admin.MAX_ROSTER_BYTES
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.admin.ROSTER_TEMPLATE
import com.mowtiie.mesgard.domain.admin.RosterEntry
import com.mowtiie.mesgard.domain.admin.RosterFileProblem
import com.mowtiie.mesgard.domain.admin.RosterParse
import com.mowtiie.mesgard.domain.admin.formatCredentials
import com.mowtiie.mesgard.domain.admin.parseRoster
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val BATCH_SIZE = 50

internal enum class ImportStage {
    START,
    PREVIEW,
    IMPORTING,
    DONE,
}

internal enum class ImportMessage {
    SAVE_FAILED,
    SAVED,
}

internal data class ImportMembersUiState(
    val stage: ImportStage = ImportStage.START,
    val entries: List<RosterEntry> = emptyList(),
    val fileProblem: RosterFileProblem? = null,
    val importedCount: Int = 0,
    val results: List<MemberCreation> = emptyList(),
    val interruptedBy: AdminError? = null,
    val areCredentialsSaved: Boolean = false,
    val message: ImportMessage? = null,
) {
    val readyEntries: List<RosterEntry>
        get() = entries.filter { it.problem == null }

    val problemEntries: List<RosterEntry>
        get() = entries.filter { it.problem != null }

    val credentials: List<IssuedCredential>
        get() {
            val names = entries.associate { it.email to it.displayName }
            return results.filterIsInstance<MemberCreation.Created>()
                .map { IssuedCredential(it.email, names[it.email] ?: it.email, it.temporaryPassword) }
        }

    val skipped: List<MemberCreation.Skipped>
        get() = results.filterIsInstance<MemberCreation.Skipped>()

    /** Passwords are shown once, so leaving with unsaved ones needs a confirmation. */
    val needsLeaveWarning: Boolean
        get() = stage == ImportStage.DONE && credentials.isNotEmpty() && !areCredentialsSaved
}

@HiltViewModel
internal class ImportMembersViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val adminRepository: AdminRepository,
    private val contactRepository: ContactRepository,
    private val documentStore: DocumentStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ImportMembersUiState())
    val uiState: StateFlow<ImportMembersUiState> = mutableUiState.asStateFlow()

    fun onFileChosen(uri: String) {
        viewModelScope.launch {
            val text = documentStore.readText(uri, MAX_ROSTER_BYTES)
            val parse = if (text == null) RosterParse.Rejected(RosterFileProblem.UNREADABLE) else parseRoster(text)
            mutableUiState.update {
                when (parse) {
                    is RosterParse.Parsed ->
                        it.copy(stage = ImportStage.PREVIEW, entries = parse.entries, fileProblem = null)
                    is RosterParse.Rejected -> it.copy(stage = ImportStage.START, fileProblem = parse.problem)
                }
            }
        }
    }

    fun onChooseAnotherFile() = mutableUiState.update { ImportMembersUiState() }

    fun onImportClicked() {
        val ready = mutableUiState.value.readyEntries
        if (mutableUiState.value.stage != ImportStage.PREVIEW || ready.isEmpty()) return
        mutableUiState.update { it.copy(stage = ImportStage.IMPORTING, importedCount = 0, results = emptyList()) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val interruptedBy = if (organizationId ==
                null
            ) {
                AdminError.NOT_ALLOWED
            } else {
                importInBatches(organizationId, ready)
            }
            if (mutableUiState.value.results.any { it is MemberCreation.Created }) contactRepository.refresh()
            mutableUiState.update { it.copy(stage = ImportStage.DONE, interruptedBy = interruptedBy) }
        }
    }

    /** Stops at the first batch that fails. The logins created so far stay, and are listed in the result. */
    private suspend fun importInBatches(organizationId: String, ready: List<RosterEntry>): AdminError? {
        for (batch in ready.chunked(BATCH_SIZE)) {
            val members = batch.map { NewMember(it.email, it.displayName, it.role) }
            when (val result = adminRepository.createMembers(organizationId, members)) {
                is AdminResult.Failure -> return result.error
                is AdminResult.Success -> mutableUiState.update {
                    it.copy(importedCount = it.importedCount + batch.size, results = it.results + result.value)
                }
            }
        }
        return null
    }

    fun onCredentialsCopied() = mutableUiState.update { it.copy(areCredentialsSaved = true) }

    fun onCredentialsDestinationChosen(uri: String) {
        val text = formatCredentials(mutableUiState.value.credentials)
        write(uri, text) { it.copy(areCredentialsSaved = true) }
    }

    fun onTemplateDestinationChosen(uri: String) = write(uri, ROSTER_TEMPLATE) { it }

    fun onMessageShown() = mutableUiState.update { it.copy(message = null) }

    private fun write(uri: String, text: String, onSaved: (ImportMembersUiState) -> ImportMembersUiState) {
        viewModelScope.launch {
            val saved = documentStore.writeText(uri, text)
            mutableUiState.update {
                if (saved) {
                    onSaved(
                        it,
                    ).copy(message = ImportMessage.SAVED)
                } else {
                    it.copy(message = ImportMessage.SAVE_FAILED)
                }
            }
        }
    }
}
