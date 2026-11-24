package com.mowtiie.mesgard.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.labelResource
import com.mowtiie.mesgard.ui.reports.ReportSheetHost

private val ProfileAvatarSize = 96.dp
private val PagePadding = 24.dp
private val Spacing = 8.dp
private val ButtonIconSpacing = ButtonDefaults.IconSpacing
private val ButtonIconSize = ButtonDefaults.IconSize

@Composable
internal fun ProfileScreenRoute(
    onBack: () -> Unit,
    onConversationStarted: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.profile.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.conversationToOpen) {
        uiState.conversationToOpen?.let {
            onConversationStarted(it)
            viewModel.onConversationOpened()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val startFailure = stringResource(R.string.start_conversation_error_unknown)
    LaunchedEffect(uiState.startFailed) {
        if (uiState.startFailed) {
            snackbarHostState.showSnackbar(startFailure)
            viewModel.onStartFailureShown()
        }
    }
    TitledScreen(
        title = stringResource(R.string.profile_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val person = uiState.person
        when {
            person != null -> ScrollingPage(padding) {
                ProfileContent(
                    person = person,
                    uiState = uiState,
                    onMessage = viewModel::onMessageClicked,
                    onReport = viewModel::onReportRequested,
                )
            }
            !uiState.isLoading -> EmptyState(
                R.drawable.illustration_contacts,
                R.string.profile_not_found,
                R.string.profile_not_found_hint,
                Modifier.padding(padding),
            )
        }
    }
    ReportSheetHost(viewModel)
}

@Composable
private fun ProfileContent(person: Contact, uiState: ProfileUiState, onMessage: () -> Unit, onReport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing),
    ) {
        InitialAvatar(person.displayName, size = ProfileAvatarSize)
        Text(
            text = person.displayName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (person.username.isNotEmpty()) {
            Text(
                text = stringResource(R.string.contact_username, person.username),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(person.role.labelResource()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.isOwnProfile) {
            Text(
                text = stringResource(R.string.profile_own),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ProfileActions(uiState.isStartingConversation, onMessage, onReport)
        }
    }
}

@Composable
private fun ProfileActions(isStartingConversation: Boolean, onMessage: () -> Unit, onReport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = PagePadding),
        verticalArrangement = Arrangement.spacedBy(Spacing),
    ) {
        Button(onClick = onMessage, enabled = !isStartingConversation, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Text(stringResource(R.string.profile_message), modifier = Modifier.padding(start = ButtonIconSpacing))
        }
        OutlinedButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Text(stringResource(R.string.profile_report), modifier = Modifier.padding(start = ButtonIconSpacing))
        }
    }
}
