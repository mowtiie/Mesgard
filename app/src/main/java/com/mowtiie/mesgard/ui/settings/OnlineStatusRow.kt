package com.mowtiie.mesgard.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import com.mowtiie.mesgard.domain.repository.onlineStatusDefault
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/** Whether the person is shown online: their own choice, or the default of their role until they choose. */
@HiltViewModel
internal class OnlineStatusViewModel @Inject constructor(
    private val onlineStatus: OnlineStatusRepository,
    accountRepository: AccountRepository,
) : ViewModel() {
    val isShown: StateFlow<Boolean> = combine(onlineStatus.choice, accountRepository.observeAccount()) {
            choice,
            account,
        ->
        choice ?: onlineStatusDefault(isStudent = account.isOnlyStudent())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), false)

    init {
        viewModelScope.launch { onlineStatus.refresh() }
    }

    fun onChanged(show: Boolean) {
        viewModelScope.launch { onlineStatus.setChoice(show) }
    }
}

private fun Account?.isOnlyStudent() = this != null &&
    memberships.isNotEmpty() &&
    memberships.all { it.role == OrgRole.MEMBER }

@Composable
internal fun OnlineStatusRow(viewModel: OnlineStatusViewModel = hiltViewModel()) {
    val isShown by viewModel.isShown.collectAsStateWithLifecycle()
    SwitchRow(
        title = R.string.settings_online_status,
        summary = R.string.settings_online_status_summary,
        checked = isShown,
        onCheckedChange = viewModel::onChanged,
    )
}
