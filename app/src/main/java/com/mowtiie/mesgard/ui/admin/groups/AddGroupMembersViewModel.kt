package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.ui.common.matchesQuery
import com.mowtiie.mesgard.ui.navigation.AddGroupMembersRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

internal data class AddGroupMembersUiState(
    val candidates: List<Contact> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
    val error: AdminError? = null,
) {
    val canAdd: Boolean
        get() = selectedIds.isNotEmpty() && !isSaving
}

/** Lists the members of the organization who are not in the group yet, to add several at once. */
@HiltViewModel
internal class AddGroupMembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeAdminOrganization: ObserveAdminOrganization,
    contactRepository: ContactRepository,
    private val groupAdminRepository: GroupAdminRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<AddGroupMembersRoute>().groupId
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<AddGroupMembersUiState> = combine(
        observeAdminOrganization.organization,
        contactRepository.observeContacts(),
        localState,
    ) { organization, contacts, local ->
        AddGroupMembersUiState(
            candidates = contacts.filter {
                it.orgId == organization?.organizationId &&
                    it.userId !in local.existingIds &&
                    matchesQuery(it.displayName, local.query)
            },
            selectedIds = local.selectedIds,
            query = local.query,
            isLoading = local.isLoading,
            isSaving = local.isSaving,
            isDone = local.isDone,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), AddGroupMembersUiState())

    init {
        viewModelScope.launch {
            when (val result = groupAdminRepository.fetchGroup(groupId)) {
                is AdminResult.Success -> localState.update {
                    it.copy(
                        existingIds = result.value.members.map { member ->
                            member.userId
                        }.toSet(),
                        isLoading = false,
                    )
                }
                is AdminResult.Failure -> localState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onQueryChanged(query: String) = localState.update { it.copy(query = query) }

    fun onToggled(userId: String) = localState.update {
        it.copy(selectedIds = if (userId in it.selectedIds) it.selectedIds - userId else it.selectedIds + userId)
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    fun onAddClicked() {
        val selected = localState.value.selectedIds
        if (selected.isEmpty() || localState.value.isSaving) return
        localState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val result = organizationId?.let { groupAdminRepository.addMembers(it, groupId, selected.toList()) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            val error = (result as? AdminResult.Failure)?.error
            localState.update { it.copy(isSaving = false, isDone = error == null, error = error) }
        }
    }

    private data class LocalState(
        val existingIds: Set<String> = emptySet(),
        val selectedIds: Set<String> = emptySet(),
        val query: String = "",
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isDone: Boolean = false,
        val error: AdminError? = null,
    )
}
