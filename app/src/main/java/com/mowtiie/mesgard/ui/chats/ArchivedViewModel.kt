package com.mowtiie.mesgard.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

internal data class ArchivedUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
internal class ArchivedViewModel @Inject constructor(repository: ConversationRepository) : ViewModel() {
    val uiState: StateFlow<ArchivedUiState> = repository.observeArchived()
        .map { ArchivedUiState(it, isLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), ArchivedUiState())
}
