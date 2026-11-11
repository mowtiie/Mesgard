package com.mowtiie.mesgard.ui.connectivity

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.network.ConnectionStatus

/** How the app reaches its server. Provided once at the root so every screen can react to it. */
internal val LocalConnectionStatus = compositionLocalOf { ConnectionStatus.ONLINE }

@Composable
internal fun ConnectionBanner(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    @StringRes offlineMessage: Int = R.string.offline_banner,
) {
    AnimatedVisibility(
        visible = status != ConnectionStatus.ONLINE,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(bannerText(status, offlineMessage)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
    }
}

@StringRes
private fun bannerText(status: ConnectionStatus, @StringRes offlineMessage: Int) = when (status) {
    ConnectionStatus.SERVER_UNREACHABLE -> R.string.server_unreachable_banner
    else -> offlineMessage
}
