package com.mowtiie.mesgard.ui.admin.usernames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class UsernamePolicyUiState(
    val policy: UsernamePolicy = UsernamePolicy.EVERYONE,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
)

/** An organization admin chooses who may change their own username. */
@HiltViewModel
internal class UsernamePolicyViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: UsernameRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(UsernamePolicyUiState())
    val uiState: StateFlow<UsernamePolicyUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null

    init {
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.rules(it) } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(policy = result.value.policy, isLoading = false)
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onSelected(policy: UsernamePolicy) = mutableUiState.update { it.copy(policy = policy) }

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
