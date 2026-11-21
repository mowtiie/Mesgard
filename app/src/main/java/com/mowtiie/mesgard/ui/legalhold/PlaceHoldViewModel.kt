package com.mowtiie.mesgard.ui.legalhold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PlaceHoldUiState(
    val isPlacing: Boolean = false,
    val isPlaced: Boolean = false,
    val error: AdminError? = null,
)

/** Places a legal hold on a person, a group or the whole organization, for the organization the person administers. */
@HiltViewModel
internal class PlaceHoldViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: LegalHoldRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PlaceHoldUiState())
    val uiState: StateFlow<PlaceHoldUiState> = mutableUiState.asStateFlow()

    fun onPlaceClicked(target: HoldTarget, reason: String) {
        mutableUiState.update { PlaceHoldUiState(isPlacing = true) }
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId
            val result =
                orgId?.let { repository.place(it, target, reason) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> PlaceHoldUiState(isPlaced = true)
                    is AdminResult.Failure -> PlaceHoldUiState(error = result.error)
                }
            }
        }
    }

    /** The screen has seen that the hold was placed, so the next dialog starts clean. */
    fun onPlacedHandled() = mutableUiState.update { PlaceHoldUiState() }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
