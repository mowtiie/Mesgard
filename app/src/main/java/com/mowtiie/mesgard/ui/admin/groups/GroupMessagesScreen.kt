package com.mowtiie.mesgard.ui.admin.groups

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AccessedMessage
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen

private const val DATE_AND_TIME =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
private val ContentPadding = 16.dp

/** Read only. The members of the group can see that an administrator opened it, and the audit log has the reason. */
@Composable
internal fun GroupMessagesScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupMessagesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }
    TitledScreen(
        title = stringResource(R.string.group_messages_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item(key = "note") {
                Text(
                    text = stringResource(R.string.group_messages_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ContentPadding),
                )
            }
            if (!uiState.isLoading && uiState.messages.isEmpty() && uiState.error == null) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.group_messages_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(ContentPadding),
                    )
                }
            }
            if (uiState.isLoading) item(key = "loading") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            items(uiState.messages, key = { it.id }) { MessageRow(it) }
            if (uiState.hasMore && !uiState.isLoading) {
                item(key = "more") {
                    OutlinedButton(
                        onClick = viewModel::loadMore,
                        modifier = Modifier.fillMaxWidth().padding(ContentPadding),
                    ) {
                        Text(stringResource(R.string.group_messages_older))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: AccessedMessage) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val time = DateUtils.formatDateTime(context, message.createdAtEpochMillis, DATE_AND_TIME)
    val sender = message.senderName ?: stringResource(R.string.group_unknown_member)
    ListItem(
        overlineContent = { Text(stringResource(R.string.group_messages_sender_time, sender, time)) },
        headlineContent = {
            Text(
                when {
                    message.isDeleted -> stringResource(R.string.group_messages_deleted)
                    message.body.isNullOrBlank() && message.hasAttachments -> stringResource(
                        R.string.group_messages_file,
                    )
                    else -> message.body.orEmpty()
                },
            )
        },
    )
}
