package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class GroupCreationUiState(
    val policy: GroupCreationPolicy = GroupCreationPolicy.ADMINS_ONLY,
    /** How many groups a person who is not an admin may create. */
    val limit: Int? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
)

/** An organization admin chooses who may create groups. */
@HiltViewModel
internal class GroupCreationViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: GroupCreationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GroupCreationUiState())
    val uiState: StateFlow<GroupCreationUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null

    init {
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.rules(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        policy = result.value.policy,
                        limit = result.value.limit,
                        isLoading = false,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onSelected(policy: GroupCreationPolicy) = mutableUiState.update { it.copy(policy = policy) }

    fun onSaveClicked() {
        val orgId = organizationId ?: return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.setPolicy(orgId, mutableUiState.value.policy)
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
