package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.MessageReader
import com.mowtiie.mesgard.ui.common.AvatarImage

private val SheetPadding = 24.dp
private val SheetSpacing = 16.dp
private const val READ_TIME_FLAGS =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH

/** Who has read a message, for its sender: the people and the time each one read it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageInfoSheet(state: ReadersState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = SheetPadding),
            verticalArrangement = Arrangement.spacedBy(SheetSpacing),
        ) {
            val count = (state as? ReadersState.Loaded)?.readers?.size
            Text(
                text = if (count == null) {
                    stringResource(R.string.message_info_title)
                } else {
                    pluralStringResource(R.plurals.message_seen_by, count, count)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = SheetPadding).semantics { heading() },
            )
            when (state) {
                ReadersState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.padding(horizontal = SheetPadding),
                )
                ReadersState.Failed -> Notice(R.string.message_info_error)
                is ReadersState.Loaded -> if (state.readers.isEmpty()) {
                    Notice(
                        R.string.message_info_empty,
                    )
                } else {
                    ReaderList(state.readers)
                }
            }
        }
    }
}

@Composable
private fun Notice(message: Int) {
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SheetPadding),
    )
}

@Composable
private fun ReaderList(readers: List<MessageReader>) {
    val context = LocalContext.current
    LazyColumn {
        items(readers, key = { it.userId }) { reader ->
            ListItem(
                leadingContent = { AvatarImage(reader.userId, reader.displayName) },
                headlineContent = { Text(reader.displayName) },
                supportingContent = {
                    Text(DateUtils.formatDateTime(context, reader.readAtEpochMillis, READ_TIME_FLAGS))
                },
            )
        }
    }
}
