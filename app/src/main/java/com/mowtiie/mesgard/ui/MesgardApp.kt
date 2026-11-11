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
import com.mowtiie.mesgard.ui.auth.SignInRoute
import com.mowtiie.mesgard.ui.connectivity.ConnectivityViewModel
import com.mowtiie.mesgard.ui.connectivity.LocalIsOffline
import com.mowtiie.mesgard.ui.lock.LockScreenRoute
import com.mowtiie.mesgard.ui.navigation.MesgardNavHost

@Composable
internal fun MesgardApp(
    conversationToOpen: String?,
    onConversationOpened: () -> Unit,
    viewModel: AppViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val isOffline by connectivityViewModel.isOffline.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalIsOffline provides isOffline) {
        when (authState) {
            AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
            AuthState.SignedOut -> SignInRoute()
            is AuthState.SignedIn -> if (isLocked) {
                LockScreenRoute()
            } else {
                MesgardNavHost(
                    onSignOut = viewModel::onSignOutClicked,
                    conversationToOpen = conversationToOpen,
                    onConversationOpened = onConversationOpened,
                )
            }
        }
    }
}
