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
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus
import com.mowtiie.mesgard.ui.lock.LockScreenRoute
import com.mowtiie.mesgard.ui.navigation.MesgardNavHost

@Composable
internal fun MesgardApp(
    conversationToOpen: String?,
    onConversationOpened: () -> Unit,
    onChangeServer: () -> Unit,
    viewModel: AppViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val connectionStatus by connectivityViewModel.status.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalConnectionStatus provides connectionStatus) {
        when (val state = authState) {
            AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
            AuthState.SignedOut -> SignInRoute(onChangeServer = onChangeServer)
            is AuthState.SignedIn -> when {
                isLocked -> LockScreenRoute()
                state.mfaStep == MfaStep.VERIFY -> MfaCodeRoute()
                state.mfaStep == MfaStep.ENROLL -> MfaEnrollmentRoute()
                state.mustChangePassword -> PasswordChangeRoute()
                else -> MesgardNavHost(
                    onSignOut = viewModel::onSignOutClicked,
                    conversationToOpen = conversationToOpen,
                    onConversationOpened = onConversationOpened,
                )
            }
        }
    }
}
