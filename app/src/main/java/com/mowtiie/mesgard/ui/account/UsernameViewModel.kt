package com.mowtiie.mesgard.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.UsernameRules
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One organization of the account and the username in it. [canChange] is false while it cannot change. */
internal data class UsernameRow(val organizationName: String, val rules: UsernameRules) {
    val canChange: Boolean
        get() = rules.policyAllowsChange && rules.nextChangeAtEpochMillis == null
}

internal data class UsernameEdit(
    val row: UsernameRow,
    val text: String,
    val isSaving: Boolean = false,
    val error: AdminError? = null,
)

internal data class UsernameUiState(val rows: List<UsernameRow> = emptyList(), val edit: UsernameEdit? = null)

/** The username of the signed in person in each organization, and changing it. A row with no answer is left out. */
@HiltViewModel
internal class UsernameViewModel @Inject constructor(private val repository: UsernameRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(UsernameUiState())
    val uiState: StateFlow<UsernameUiState> = mutableUiState.asStateFlow()
    private var memberships: List<AccountMembership>? = null

    fun onMembershipsChanged(changed: List<AccountMembership>) {
        if (changed == memberships) return
        memberships = changed
        reload()
    }

    fun onEditRequested(row: UsernameRow) = mutableUiState.update {
        it.copy(edit = UsernameEdit(row, row.rules.username))
    }

    fun onTextChanged(text: String) = mutableUiState.update { state ->
        state.copy(edit = state.edit?.copy(text = text.take(USERNAME_INPUT_LIMIT), error = null))
    }

    fun onEditDismissed() = mutableUiState.update { it.copy(edit = null) }

    fun onSaveClicked() {
        val edit = mutableUiState.value.edit ?: return
        if (edit.isSaving) return
        mutableUiState.update { it.copy(edit = edit.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            when (val result = repository.change(edit.row.rules.orgId, edit.text)) {
                is AdminResult.Success -> {
                    mutableUiState.update { it.copy(edit = null) }
                    reload()
                }
                is AdminResult.Failure ->
                    mutableUiState.update { it.copy(edit = edit.copy(isSaving = false, error = result.error)) }
            }
        }
    }

    private fun reload() {
        val current = memberships ?: return
        viewModelScope.launch { mutableUiState.update { it.copy(rows = loadRows(current)) } }
    }

    private suspend fun loadRows(current: List<AccountMembership>): List<UsernameRow> = coroutineScope {
        current.map { membership ->
            async {
                (repository.rules(membership.organizationId) as? AdminResult.Success)?.value
                    ?.let { UsernameRow(membership.organizationName, it) }
            }
        }.mapNotNull { it.await() }
    }
}

private const val USERNAME_INPUT_LIMIT = 40
