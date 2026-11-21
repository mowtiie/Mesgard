package com.mowtiie.mesgard.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.LoadRuleSummaries
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.admin.RuleSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the organization has set under Rules, read again each time the page comes back into view. */
@HiltViewModel
internal class AdministrationSummaryViewModel @Inject constructor(
    private val observeAdminOrganization: ObserveAdminOrganization,
    private val loadRuleSummaries: LoadRuleSummaries,
) : ViewModel() {
    private val mutableSummaries = MutableStateFlow(RuleSummaries())
    val summaries: StateFlow<RuleSummaries> = mutableSummaries.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val organizationId = observeAdminOrganization.organization.first()?.organizationId ?: return@launch
            mutableSummaries.value = loadRuleSummaries(organizationId)
        }
    }
}
