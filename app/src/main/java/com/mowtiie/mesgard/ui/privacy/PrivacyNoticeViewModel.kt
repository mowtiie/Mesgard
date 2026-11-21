package com.mowtiie.mesgard.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PrivacyNoticeUiState(
    val notices: List<PrivacyNotice> = emptyList(),
    val isLoading: Boolean = true,
    val isAcknowledging: Boolean = false,
    val error: AdminError? = null,
) {
    /** The first notice the person has not acknowledged yet. It is shown before they go on. */
    val pending: PrivacyNotice?
        get() = notices.firstOrNull { it.needsAcknowledgment }
}

/**
 * The privacy notices of the person's organizations. When the server cannot be asked the person is not held back:
 * the notice is shown the next time it can be.
 */
@HiltViewModel
internal class PrivacyNoticeViewModel @Inject constructor(private val repository: PrivacyNoticeRepository) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(PrivacyNoticeUiState())
    val uiState: StateFlow<PrivacyNoticeUiState> = mutableUiState.asStateFlow()

    init {
        onRefresh()
    }

    fun onRefresh() {
        mutableUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.notices()) {
                is AdminResult.Success -> mutableUiState.update { it.copy(notices = result.value, isLoading = false) }
                is AdminResult.Failure -> mutableUiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onAcknowledged(orgId: String) {
        mutableUiState.update { it.copy(isAcknowledging = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.acknowledge(orgId)) {
                is AdminResult.Success -> {
                    mutableUiState.update { it.copy(isAcknowledging = false) }
                    onRefresh()
                }
                is AdminResult.Failure -> mutableUiState.update {
                    it.copy(isAcknowledging = false, error = result.error)
                }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
