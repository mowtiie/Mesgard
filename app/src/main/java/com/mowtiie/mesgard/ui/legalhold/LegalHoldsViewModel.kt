package com.mowtiie.mesgard.ui.legalhold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.LegalHold
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LegalHoldsUiState(
    val holds: List<LegalHold> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val holdToRelease: LegalHold? = null,
    val error: AdminError? = null,
) {
    val showEmptyMessage: Boolean
        get() = holds.isEmpty() && !isLoading && error == null
}

/** The legal holds that are active, and releasing one with a note. */
@HiltViewModel
internal class LegalHoldsViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: LegalHoldRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LegalHoldsUiState())
    val uiState: StateFlow<LegalHoldsUiState> = mutableUiState.asStateFlow()

    init {
        onRefresh()
    }

    fun onRefresh() {
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId
            val result = orgId?.let { repository.activeHolds(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(holds = result.value, isLoading = false)
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onReleaseRequested(hold: LegalHold) = mutableUiState.update { it.copy(holdToRelease = hold) }

    fun onReleaseDismissed() = mutableUiState.update { it.copy(holdToRelease = null) }

    fun onReleaseConfirmed(note: String) {
        val hold = mutableUiState.value.holdToRelease ?: return
        mutableUiState.update { it.copy(holdToRelease = null, isWorking = true) }
        viewModelScope.launch {
            when (val result = repository.release(hold.id, note)) {
                is AdminResult.Success -> {
                    mutableUiState.update { it.copy(isWorking = false) }
                    onRefresh()
                }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
