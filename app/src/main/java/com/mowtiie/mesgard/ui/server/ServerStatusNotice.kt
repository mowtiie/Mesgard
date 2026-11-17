package com.mowtiie.mesgard.ui.server

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ServerStatus

/** Whether the server has what this version of the app needs. Provided once at the root. */
internal val LocalServerStatus = compositionLocalOf { ServerStatus.UNKNOWN }

private val NoticePadding = 16.dp
private val NoticeSpacing = 4.dp

/**
 * Says the server is older than the app, and who can fix it. It shows nothing while the server is current or when it
 * could not be asked, so a flaky connection never raises a false alarm.
 */
@Composable
internal fun ServerStatusNotice(forAdministrator: Boolean, modifier: Modifier = Modifier) {
    if (LocalServerStatus.current != ServerStatus.OUTDATED) return
    @StringRes val message = if (forAdministrator) R.string.server_outdated_admin else R.string.server_outdated_user
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(NoticePadding), verticalArrangement = Arrangement.spacedBy(NoticeSpacing)) {
            Text(
                text = stringResource(R.string.server_outdated_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
