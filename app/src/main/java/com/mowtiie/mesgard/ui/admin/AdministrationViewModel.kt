package com.mowtiie.mesgard.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditEntry
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.domain.repository.ROSTER_LIMIT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
internal const val RECENT_ACTIVITY_COUNT = 3

internal data class AdministrationUiState(
    val isAdmin: Boolean = false,
    val organizationName: String? = null,
    /** The people of the stored roster. The roster is limited, so a count at the limit may be higher in reality. */
    val memberCount: Int? = null,
    val groupCount: Int? = null,
    val recentActivity: List<AuditEntry> = emptyList(),
    val hasLoadedActivity: Boolean = false,
    val names: Map<String, String> = emptyMap(),
    val isRefreshing: Boolean = true,
    val error: AdminError? = null,
) {
    val isMemberCountAtLeast: Boolean
        get() = (memberCount ?: 0) >= ROSTER_LIMIT

    val showNoActivity: Boolean
        get() = hasLoadedActivity && recentActivity.isEmpty()
}

/** The overview of the organization for its admins: counts, the latest changes and the way to each admin screen. */
@HiltViewModel
internal class AdministrationViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val contactRepository: ContactRepository,
    private val groupAdminRepository: GroupAdminRepository,
    private val auditLogRepository: AuditLogRepository,
    accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<AdministrationUiState> = combine(
        observeAdminOrganization.organization,
        contactRepository.observeContacts(),
        accountRepository.observeAccount(),
        localState,
    ) { organization, contacts, account, local ->
        val own = authRepository.currentUserId?.let { id -> account?.let { mapOf(id to it.displayName) } }.orEmpty()
        AdministrationUiState(
            isAdmin = organization != null,
            organizationName = organization?.organizationName,
            memberCount = organization?.let { admin -> contacts.count { it.orgId == admin.organizationId } },
            groupCount = local.groupCount,
            recentActivity = local.recentActivity,
            hasLoadedActivity = local.hasLoadedActivity,
            names = contacts.associate { it.userId to it.displayName } + own,
            isRefreshing = local.isRefreshing,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), AdministrationUiState())

    init {
        onRefresh()
    }

    fun onRefresh() {
        localState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId
            if (organizationId == null) {
                localState.update { it.copy(isRefreshing = false, error = AdminError.NOT_ALLOWED) }
            } else {
                load(organizationId)
            }
        }
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    private suspend fun load(organizationId: String) = coroutineScope {
        val roster = async { contactRepository.refresh() }
        val groups = async { groupAdminRepository.listGroups(organizationId) }
        val activity = async { auditLogRepository.fetchPage(organizationId, before = null) }
        roster.await()
        val groupsResult = groups.await()
        val activityResult = activity.await()
        localState.update { state ->
            state.copy(
                groupCount = (groupsResult as? AdminResult.Success)?.value?.size ?: state.groupCount,
                recentActivity = (activityResult as? AdminResult.Success)?.value?.entries?.take(RECENT_ACTIVITY_COUNT)
                    ?: state.recentActivity,
                hasLoadedActivity = state.hasLoadedActivity || activityResult is AdminResult.Success,
                isRefreshing = false,
                error = listOf(groupsResult, activityResult).filterIsInstance<AdminResult.Failure>()
                    .firstOrNull()?.error,
            )
        }
    }

    private data class LocalState(
        val groupCount: Int? = null,
        val recentActivity: List<AuditEntry> = emptyList(),
        val hasLoadedActivity: Boolean = false,
        val isRefreshing: Boolean = true,
        val error: AdminError? = null,
    )
}
