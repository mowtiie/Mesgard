package com.mowtiie.mesgard.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Puts the privacy notice in front of the app until the person acknowledges it. The app shows at once and the notice
 * comes in front when the server says it is due, so a phone without a connection is never held back. The state is
 * kept per person, so the next person to sign in on this phone never sees what was due for the last one.
 */
@Composable
internal fun PrivacyNoticeGate(
    userId: String,
    content: @Composable () -> Unit,
    viewModel: PrivacyNoticeViewModel = hiltViewModel(key = userId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pending = uiState.pending
    if (pending == null) {
        content()
    } else {
        PrivacyNoticeGateScreen(
            notice = pending,
            isAcknowledging = uiState.isAcknowledging,
            error = uiState.error,
            onContinue = { viewModel.onAcknowledged(pending.orgId) },
        )
    }
}
