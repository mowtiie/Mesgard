package com.mowtiie.mesgard.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.DEFAULT_QUIET_END_HOUR
import com.mowtiie.mesgard.domain.model.DEFAULT_QUIET_START_HOUR
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MessagingRulesUiState(
    val rules: MessagingRules = MessagingRules(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && rules.isValid
}

/** An organization admin turns the messaging rules on or off. */
@HiltViewModel
internal class MessagingRulesViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: MessagingRulesRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MessagingRulesUiState())
    val uiState: StateFlow<MessagingRulesUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null

    init {
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.rules(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(rules = result.value, isLoading = false)
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onBlockChanged(block: Boolean) = changeRules { it.copy(blockStaffStudentDirectMessages = block) }

    /** Turning quiet hours on starts from 21:00 to 06:00, which a person then adjusts. */
    fun onQuietHoursChanged(isOn: Boolean) = changeRules {
        if (isOn) {
            it.copy(
                quietStart = it.quietStart ?: LocalTime.of(DEFAULT_QUIET_START_HOUR, 0),
                quietEnd = it.quietEnd ?: LocalTime.of(DEFAULT_QUIET_END_HOUR, 0),
            )
        } else {
            it.copy(quietStart = null, quietEnd = null)
        }
    }

    fun onQuietStartChanged(time: LocalTime) = changeRules { it.copy(quietStart = time) }

    fun onQuietEndChanged(time: LocalTime) = changeRules { it.copy(quietEnd = time) }

    fun onSaveClicked() {
        val orgId = organizationId ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.setRules(orgId, state.rules)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AdminResult.Failure -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    private fun changeRules(change: (MessagingRules) -> MessagingRules) =
        mutableUiState.update { it.copy(rules = change(it.rules)) }
}
