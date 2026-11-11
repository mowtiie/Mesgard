package com.mowtiie.mesgard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.ui.auth.SignInRoute
import com.mowtiie.mesgard.ui.lock.LockScreenRoute
import com.mowtiie.mesgard.ui.navigation.MesgardNavHost

@Composable
internal fun MesgardApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
        AuthState.SignedOut -> SignInRoute()
        is AuthState.SignedIn -> if (isLocked) {
            LockScreenRoute()
        } else {
            MesgardNavHost(onSignOut = viewModel::onSignOutClicked)
        }
    }
}
