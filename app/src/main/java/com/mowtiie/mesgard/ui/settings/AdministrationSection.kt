package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class AdministrationViewModel @Inject constructor(observeAdminOrganization: ObserveAdminOrganization) :
    ViewModel() {
    val isAdmin: StateFlow<Boolean> = observeAdminOrganization.organization
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), false)
}

/** Only the admin of an organization sees this section. */
@Composable
internal fun AdministrationSection(onOpenMembers: () -> Unit, viewModel: AdministrationViewModel = hiltViewModel()) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    if (!isAdmin) return
    SectionHeader(R.string.settings_administration)
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onOpenMembers),
        headlineContent = { Text(stringResource(R.string.members_title)) },
        supportingContent = { Text(stringResource(R.string.settings_members_summary)) },
    )
}
