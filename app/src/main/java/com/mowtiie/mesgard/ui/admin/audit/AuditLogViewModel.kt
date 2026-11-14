package com.mowtiie.mesgard.ui.admin.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditCursor
import com.mowtiie.mesgard.domain.model.AuditEntry
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
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

internal data class AuditLogUiState(
    val entries: List<AuditEntry> = emptyList(),
    val names: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val error: AdminError? = null,
) {
    val showEmptyMessage: Boolean
        get() = entries.isEmpty() && !isLoading
}

@HiltViewModel
internal class AuditLogViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val auditLogRepository: AuditLogRepository,
    contactRepository: ContactRepository,
    accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<AuditLogUiState> = combine(
        localState,
        contactRepository.observeContacts(),
        accountRepository.observeAccount(),
    ) { local, contacts, account ->
        val own = authRepository.currentUserId?.let { id -> account?.let { mapOf(id to it.displayName) } }.orEmpty()
        AuditLogUiState(
            entries = local.entries,
            names = contacts.associate { it.userId to it.displayName } + own,
            isLoading = local.isLoading,
            hasMore = local.next != null,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), AuditLogUiState())

    init {
        onRefresh()
    }

    fun onRefresh() = load(before = null)

    fun onLoadMore() {
        val next = localState.value.next ?: return
        load(before = next)
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private fun load(before: AuditCursor?) {
        if (localState.value.isLoading && before != null) return
        localState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            val result = organizationId?.let { auditLogRepository.fetchPage(it, before) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            localState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        entries = if (before == null) result.value.entries else it.entries + result.value.entries,
                        next = result.value.next,
                        isLoading = false,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private data class LocalState(
        val entries: List<AuditEntry> = emptyList(),
        val next: AuditCursor? = null,
        val isLoading: Boolean = true,
        val error: AdminError? = null,
    )
}
