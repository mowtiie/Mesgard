package com.mowtiie.mesgard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.MfaStep
import com.mowtiie.mesgard.ui.auth.MfaCodeRoute
import com.mowtiie.mesgard.ui.auth.MfaEnrollmentRoute
import com.mowtiie.mesgard.ui.auth.PasswordChangeRoute
import com.mowtiie.mesgard.ui.auth.SignInRoute
import com.mowtiie.mesgard.ui.connectivity.ConnectivityViewModel
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionRetry
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus
import com.mowtiie.mesgard.ui.lock.LockScreenRoute
import com.mowtiie.mesgard.ui.navigation.MesgardNavHost
import com.mowtiie.mesgard.ui.privacy.PrivacyNoticeGate
import com.mowtiie.mesgard.ui.server.LocalServerStatus
import com.mowtiie.mesgard.ui.server.ServerStatusViewModel

@Composable
internal fun MesgardApp(
    conversationToOpen: String?,
    onConversationOpened: () -> Unit,
    onChangeServer: () -> Unit,
    viewModel: AppViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
    serverStatusViewModel: ServerStatusViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val connectionStatus by connectivityViewModel.status.collectAsStateWithLifecycle()
    val serverStatus by serverStatusViewModel.status.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalConnectionStatus provides connectionStatus,
        LocalConnectionRetry provides connectivityViewModel::onRetryClicked,
        LocalServerStatus provides serverStatus,
    ) {
        when (val state = authState) {
            AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
            AuthState.SignedOut -> SignInRoute(onChangeServer = onChangeServer)
            is AuthState.SignedIn -> when {
                isLocked -> LockScreenRoute()
                state.mfaStep == MfaStep.VERIFY -> MfaCodeRoute()
                state.mfaStep == MfaStep.ENROLL -> MfaEnrollmentRoute()
                state.mustChangePassword -> PasswordChangeRoute()
                else -> PrivacyNoticeGate(userId = state.userId, content = {
                    MesgardNavHost(
                        onSignOut = viewModel::onSignOutClicked,
                        conversationToOpen = conversationToOpen,
                        onConversationOpened = onConversationOpened,
                    )
                })
            }
        }
    }
}
