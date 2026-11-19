package com.mowtiie.mesgard.ui.connectivity

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.network.ConnectionStatus

/** How the app reaches its server. Provided once at the root so every screen can react to it. */
internal val LocalConnectionStatus = compositionLocalOf { ConnectionStatus.ONLINE }

/** Tries to reach the server again. Provided once at the root, next to [LocalConnectionStatus]. */
internal val LocalConnectionRetry = compositionLocalOf { {} }

/**
 * A snackbar that stays while the app cannot reach its server, with a Retry action, and goes away when the connection
 * is back. It sits in the snackbar slot of a screen, so it stays above the navigation bar.
 */
@Composable
internal fun ConnectionSnackbarHost(
    @StringRes offlineMessage: Int = R.string.offline_banner,
    modifier: Modifier = Modifier,
) {
    val status = LocalConnectionStatus.current
    val onRetry = LocalConnectionRetry.current
    val hostState = remember { SnackbarHostState() }
    val message = stringResource(snackbarText(status, offlineMessage))
    val retryLabel = stringResource(R.string.connection_retry)
    LaunchedEffect(status) {
        if (status == ConnectionStatus.ONLINE) return@LaunchedEffect
        while (true) {
            val result = hostState.showSnackbar(message, retryLabel, duration = SnackbarDuration.Indefinite)
            if (result == SnackbarResult.ActionPerformed) onRetry()
        }
    }
    SnackbarHost(hostState, modifier)
}

@StringRes
private fun snackbarText(status: ConnectionStatus, @StringRes offlineMessage: Int) = when (status) {
    ConnectionStatus.SERVER_UNREACHABLE -> R.string.server_unreachable_banner
    else -> offlineMessage
}
