package com.mowtiie.mesgard.ui.admin.former

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.FormerMember
import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.FormerMembersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class FormerMembersUiState(
    val members: List<FormerMember> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val erasureMode: ErasureMode = ErasureMode.OFF,
    val confirming: FormerMember? = null,
    val isWorking: Boolean = false,
    val error: AdminError? = null,
) {
    val showEmptyMessage: Boolean
        get() = members.isEmpty() && !isLoading

    /** The Erase action is only offered when the server allows erasing and the person can still be erased. */
    fun canErase(member: FormerMember): Boolean = member.canErase && erasureMode != ErasureMode.OFF
}

/** Lists the people the organization removed, and finishes an erase that did not complete. */
@HiltViewModel
internal class FormerMembersViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val formerMembersRepository: FormerMembersRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FormerMembersUiState())
    val uiState: StateFlow<FormerMembersUiState> = mutableUiState.asStateFlow()
    private var next: FormerMemberCursor? = null

    init {
        viewModelScope.launch {
            val mode = (adminRepository.erasureMode() as? AdminResult.Success)?.value ?: ErasureMode.OFF
            mutableUiState.update { it.copy(erasureMode = mode) }
        }
        onRefresh()
    }

    fun onRefresh() = load(before = null)

    fun onLoadMore() {
        load(before = next ?: return)
    }

    fun onEraseClicked(member: FormerMember) = mutableUiState.update { it.copy(confirming = member) }

    fun onEraseDismissed() = mutableUiState.update { it.copy(confirming = null) }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onEraseConfirmed() {
        val member = mutableUiState.value.confirming ?: return
        if (mutableUiState.value.isWorking) return
        mutableUiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = organizationId()?.let { adminRepository.eraseMember(it, member.userId) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                it.copy(
                    isWorking = false,
                    confirming = null,
                    error = (result as? AdminResult.Failure)?.error,
                )
            }
            if (result is AdminResult.Success) onRefresh()
        }
    }

    private fun load(before: FormerMemberCursor?) {
        if (mutableUiState.value.isLoading && before != null) return
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = organizationId()?.let { formerMembersRepository.fetchPage(it, before) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            when (result) {
                is AdminResult.Success -> {
                    next = result.value.next
                    mutableUiState.update {
                        it.copy(
                            members = if (before == null) result.value.members else it.members + result.value.members,
                            hasMore = result.value.next != null,
                            isLoading = false,
                        )
                    }
                }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    private suspend fun organizationId(): String? = observeAdminOrganization.organization.first()?.organizationId
}
