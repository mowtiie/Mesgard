package com.mowtiie.mesgard.ui.settings

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
internal class AdministrationEntryViewModel @Inject constructor(observeAdminOrganization: ObserveAdminOrganization) :
    ViewModel() {
    val isAdmin: StateFlow<Boolean> = observeAdminOrganization.organization
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), false)
}

/** The way into the Administration page. Only the admin of an organization sees it. */
@Composable
internal fun AdministrationEntry(onOpen: () -> Unit, viewModel: AdministrationEntryViewModel = hiltViewModel()) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    if (!isAdmin) return
    SettingsCategoryRow(
        icon = { Icon(painterResource(R.drawable.ic_shield), contentDescription = null) },
        title = R.string.settings_administration,
        summary = stringResource(R.string.settings_administration_summary),
        onClick = onOpen,
    )
}
