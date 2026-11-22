package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.ui.common.matchesQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class GroupsUiState(
    val groups: List<AdminGroup> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: AdminError? = null,
) {
    val visibleGroups: List<AdminGroup>
        get() = groups.filter { matchesQuery(it.name, query) }

    /** The whole tree, indented by depth. While searching, the matches are listed flat. */
    val rows: List<GroupRow>
        get() = if (query.isBlank()) flattenGroupTree(groups) else visibleGroups.map { GroupRow(it, depth = 0) }

    val showEmptyMessage: Boolean
        get() = groups.isEmpty() && !isLoading

    val showNoResults: Boolean
        get() = groups.isNotEmpty() && visibleGroups.isEmpty()
}

@HiltViewModel
internal class GroupsViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val groupAdminRepository: GroupAdminRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = mutableUiState.asStateFlow()

    init {
        onRefresh()
    }

    fun onQueryChanged(query: String) = mutableUiState.update { it.copy(query = query) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onRefresh() {
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val result = organizationId?.let { groupAdminRepository.listGroups(it) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(groups = result.value, isLoading = false)
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onCreateConfirmed(name: String, settings: GroupSettings = GroupSettings()) {
        if (name.isBlank() || mutableUiState.value.isWorking) return
        mutableUiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val result = organizationId?.let { groupAdminRepository.createGroup(it, name, settings) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            val error = (result as? AdminResult.Failure)?.error
            mutableUiState.update { it.copy(isWorking = false, error = error) }
            if (error == null) onRefresh()
        }
    }
}
