package com.mowtiie.mesgard.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.PersonReport
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.ui.navigation.ProfileRoute
import com.mowtiie.mesgard.ui.reports.ReportFormViewModel
import com.mowtiie.mesgard.ui.reports.ReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

internal data class ProfileUiState(
    val isLoading: Boolean = true,
    /** Null once loading is over means the person is not in the roster kept on the phone. */
    val person: Contact? = null,
    val isOwnProfile: Boolean = false,
    val isStartingConversation: Boolean = false,
    val conversationToOpen: String? = null,
    val startFailed: Boolean = false,
)

/** The profile of one person of an organization: who they are, and what you can do about them. */
@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contacts: ContactRepository,
    private val reports: ReportRepository,
    authRepository: AuthRepository,
) : ReportFormViewModel() {
    private val route = savedStateHandle.toRoute<ProfileRoute>()
    private val isOwnProfile = authRepository.currentUserId == route.userId
    private val localState = MutableStateFlow(ProfileUiState())

    val profile: StateFlow<ProfileUiState> = combine(
        contacts.observeContacts().map { roster -> roster.firstOrNull { it.matches() } },
        localState,
    ) { person, local -> local.copy(isLoading = false, person = person, isOwnProfile = isOwnProfile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), ProfileUiState())

    init {
        viewModelScope.launch { contacts.refreshIfStale() }
    }

    fun onMessageClicked() {
        val person = profile.value.person ?: return
        if (localState.value.isStartingConversation) return
        localState.update { it.copy(isStartingConversation = true, startFailed = false) }
        viewModelScope.launch {
            when (val result = contacts.startDirectConversation(person)) {
                is StartConversationResult.Started ->
                    localState.update {
                        it.copy(isStartingConversation = false, conversationToOpen = result.conversationId)
                    }
                is StartConversationResult.Failure ->
                    localState.update { it.copy(isStartingConversation = false, startFailed = true) }
            }
        }
    }

    fun onConversationOpened() = localState.update { it.copy(conversationToOpen = null) }

    fun onStartFailureShown() = localState.update { it.copy(startFailed = false) }

    fun onReportRequested() =
        open(route.userId, listOf(ReportSubject.PERSON, ReportSubject.NAME, ReportSubject.USERNAME))

    override suspend fun send(state: ReportUiState): ReportResult {
        val subject = state.subject
        val category = state.category
        val person = profile.value.person
        if (subject == null || category == null || person == null) return ReportResult.Failure(ReportError.UNKNOWN)
        return reports.reportPerson(
            PersonReport(state.reportId, person.orgId, person.userId, subject, category, state.note, state.isAnonymous),
        )
    }

    private fun Contact.matches() = userId == route.userId && (route.orgId == null || orgId == route.orgId)
}
