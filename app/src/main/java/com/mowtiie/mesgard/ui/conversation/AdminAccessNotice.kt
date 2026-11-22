package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private const val DATE_AND_TIME =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
private val NoticePadding = 12.dp

/** Members of a group a person created are told when an administrator opened its messages, and when. */
@Composable
internal fun AdminAccessNotice(openedAtEpochMillis: Long?) {
    if (openedAtEpochMillis == null) return
    val time = DateUtils.formatDateTime(LocalContext.current, openedAtEpochMillis, DATE_AND_TIME)
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.conversation_admin_access_notice, time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(NoticePadding),
        )
    }
}
