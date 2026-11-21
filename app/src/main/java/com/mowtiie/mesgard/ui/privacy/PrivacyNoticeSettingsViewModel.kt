package com.mowtiie.mesgard.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.isValidNoticeLink
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PrivacyNoticeSettingsUiState(
    val noticeUrl: String = "",
    val childNoticeUrl: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: AdminError? = null,
) {
    val isNoticeUrlValid: Boolean
        get() = isValidNoticeLink(noticeUrl)

    val isChildNoticeUrlValid: Boolean
        get() = isValidNoticeLink(childNoticeUrl)

    val canSave: Boolean
        get() = !isLoading && !isSaving && isNoticeUrlValid && isChildNoticeUrlValid
}

/** An organization admin sets the links to the privacy notice and to the version written for children. */
@HiltViewModel
internal class PrivacyNoticeSettingsViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val repository: PrivacyNoticeRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PrivacyNoticeSettingsUiState())
    val uiState: StateFlow<PrivacyNoticeSettingsUiState> = mutableUiState.asStateFlow()
    private var organizationId: String? = null

    init {
        viewModelScope.launch {
            val orgId = observeAdminOrganization.organization.first()?.organizationId?.also { organizationId = it }
            val result = orgId?.let { repository.notices() } ?: AdminResult.Failure(AdminError.NOT_ALLOWED)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> {
                        val current = result.value.firstOrNull { notice -> notice.orgId == orgId }
                        it.copy(
                            noticeUrl = current?.noticeUrl.orEmpty(),
                            childNoticeUrl = current?.childNoticeUrl.orEmpty(),
                            isLoading = false,
                        )
                    }
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onNoticeUrlChanged(value: String) = mutableUiState.update { it.copy(noticeUrl = value) }

    fun onChildNoticeUrlChanged(value: String) = mutableUiState.update { it.copy(childNoticeUrl = value) }

    fun onSaveClicked() {
        val orgId = organizationId ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.setLinks(orgId, state.noticeUrl, state.childNoticeUrl)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AdminResult.Failure -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }
}
