package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.ObservePostingOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.AnnouncementRepository
import com.mowtiie.mesgard.ui.admin.messageResource
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
private val ProgressSize = 24.dp

internal data class AnnouncementsUiState(
    val canPost: Boolean = false,
    val isOpening: Boolean = false,
    val conversationToOpen: String? = null,
    val error: AdminError? = null,
)

@HiltViewModel
internal class AnnouncementsViewModel @Inject constructor(
    private val observePostingOrganization: ObservePostingOrganization,
    private val announcementRepository: AnnouncementRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(AnnouncementsUiState())

    val uiState: StateFlow<AnnouncementsUiState> = combine(
        observePostingOrganization.organization,
        localState,
    ) { organization, local -> local.copy(canPost = organization != null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), AnnouncementsUiState())

    fun onOpenClicked() {
        if (localState.value.isOpening) return
        localState.update { it.copy(isOpening = true) }
        viewModelScope.launch {
            val organizationId = observePostingOrganization.organization.first()?.organizationId
            val result = organizationId?.let { announcementRepository.openAnnouncements(it) }
                ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            localState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isOpening = false, conversationToOpen = result.value)
                    is AdminResult.Failure -> it.copy(isOpening = false, error = result.error)
                }
            }
        }
    }

    fun onOpened() = localState.update { it.copy(conversationToOpen = null) }

    fun onErrorShown() = localState.update { it.copy(error = null) }
}

/** Admins and staff reach the announcement conversation of their organization from here. */
@Composable
internal fun AnnouncementsSection(
    snackbarHostState: SnackbarHostState,
    onOpenConversation: (String) -> Unit,
    viewModel: AnnouncementsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(uiState.conversationToOpen) {
        uiState.conversationToOpen?.let {
            onOpenConversation(it)
            viewModel.onOpened()
        }
    }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }
    if (!uiState.canPost) return
    SectionHeader(R.string.settings_announcements)
    ListItem(
        modifier = Modifier.clickable(
            enabled = !uiState.isOpening,
            role = Role.Button,
            onClick = viewModel::onOpenClicked,
        ),
        headlineContent = { Text(stringResource(R.string.announcements_post)) },
        supportingContent = { Text(stringResource(R.string.announcements_summary)) },
        trailingContent = if (uiState.isOpening) {
            { CircularProgressIndicator(modifier = Modifier.size(ProgressSize)) }
        } else {
            null
        },
    )
}
