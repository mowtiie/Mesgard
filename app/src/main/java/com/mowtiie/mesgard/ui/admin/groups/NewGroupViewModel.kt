package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupPlacement
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class NewGroupUiState(
    val name: String = "",
    val kind: GroupKind = GroupKind.OTHER,
    val settings: GroupSettings = GroupSettings(),
    val parentId: String? = null,
    /** The groups a new group can be put inside. */
    val groups: List<AdminGroup> = emptyList(),
    val isWorking: Boolean = false,
    val isCreated: Boolean = false,
    val error: AdminError? = null,
) {
    val parentName: String?
        get() = groups.firstOrNull { it.id == parentId }?.name

    val canCreate: Boolean
        get() = name.isNotBlank() && !isWorking
}

/** Creates a group: its name, its kind, where it sits in the tree and who can post. */
@HiltViewModel
internal class NewGroupViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val groupAdminRepository: GroupAdminRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NewGroupUiState())
    val uiState: StateFlow<NewGroupUiState> = mutableUiState.asStateFlow()

    init {
        loadGroups()
    }

    fun onNameChanged(name: String) = mutableUiState.update { it.copy(name = name) }

    fun onKindChanged(kind: GroupKind) = mutableUiState.update { it.copy(kind = kind) }

    fun onSettingsChanged(settings: GroupSettings) = mutableUiState.update { it.copy(settings = settings) }

    fun onParentChosen(parentId: String?) = mutableUiState.update { it.copy(parentId = parentId) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onCreateClicked() {
        val state = mutableUiState.value
        if (!state.canCreate) return
        mutableUiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = organizationId()?.let {
                groupAdminRepository.createGroup(
                    it,
                    state.name,
                    state.settings,
                    GroupPlacement(state.kind, state.parentId),
                )
            } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                it.copy(
                    isWorking = false,
                    isCreated = result is AdminResult.Success,
                    error = (result as? AdminResult.Failure)?.error,
                )
            }
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            val result = organizationId()?.let { groupAdminRepository.listGroups(it) }
            if (result is AdminResult.Success) mutableUiState.update { it.copy(groups = result.value) }
        }
    }

    private suspend fun organizationId() = observeAdminOrganization.organization.first()?.organizationId
}
