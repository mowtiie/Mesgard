package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.domain.repository.GroupTreeRepository
import com.mowtiie.mesgard.ui.navigation.GroupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class GroupTreeDialog {
    KIND,
    PARENT,
}

internal data class GroupTreeUiState(
    val groupId: String,
    val groups: List<AdminGroup> = emptyList(),
    val dialog: GroupTreeDialog? = null,
    val isWorking: Boolean = false,
    val error: AdminError? = null,
) {
    private val group: AdminGroup?
        get() = groups.firstOrNull { it.id == groupId }

    val kind: GroupKind?
        get() = group?.kind

    val parentId: String?
        get() = group?.parentId

    val parentName: String?
        get() = groups.firstOrNull { it.id == parentId }?.name

    /** A group cannot go inside itself or inside a group it contains. */
    val excludedParentIds: Set<String>
        get() = descendantsOf(groupId) + groupId

    private fun descendantsOf(id: String): Set<String> =
        groups.filter { it.parentId == id }.flatMap { setOf(it.id) + descendantsOf(it.id) }.toSet()
}

/** Where a group sits and what kind it is. Only organization admins change these. */
@HiltViewModel
internal class GroupTreeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val groupAdminRepository: GroupAdminRepository,
    private val groupTreeRepository: GroupTreeRepository,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<GroupRoute>().groupId
    private val mutableUiState = MutableStateFlow(GroupTreeUiState(groupId))
    val uiState: StateFlow<GroupTreeUiState> = mutableUiState.asStateFlow()

    init {
        loadGroups()
    }

    fun onDialogChanged(dialog: GroupTreeDialog?) = mutableUiState.update { it.copy(dialog = dialog) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onKindChosen(kind: GroupKind) = change { groupTreeRepository.changeKind(groupId, kind) }

    fun onParentChosen(parentId: String?) = change { groupTreeRepository.moveGroup(groupId, parentId) }

    private fun change(action: suspend () -> AdminResult<Unit>) {
        if (mutableUiState.value.isWorking) return
        mutableUiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val error = (action() as? AdminResult.Failure)?.error
            mutableUiState.update { it.copy(isWorking = false, dialog = null, error = error) }
            if (error == null) loadGroups()
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId ?: return@launch
            val result = groupAdminRepository.listGroups(organizationId)
            if (result is AdminResult.Success) mutableUiState.update { it.copy(groups = result.value) }
        }
    }
}
