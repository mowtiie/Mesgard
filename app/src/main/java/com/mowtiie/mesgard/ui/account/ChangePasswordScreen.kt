package com.mowtiie.mesgard.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.mowtiie.mesgard.domain.auth.MIN_PASSWORD_LENGTH
import com.mowtiie.mesgard.ui.auth.PasswordChangeUiState
import com.mowtiie.mesgard.ui.auth.PasswordChangeViewModel
import com.mowtiie.mesgard.ui.auth.PasswordForm
import com.mowtiie.mesgard.ui.auth.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

@Composable
internal fun ChangePasswordScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordChangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChangePasswordScreen(
        uiState = uiState,
        onBack = onBack,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmationChanged = viewModel::onConfirmationChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onIssueShown = viewModel::onIssueShown,
        onChangeAnnounced = viewModel::onChangeAnnounced,
        modifier = modifier,
    )
}

@Composable
internal fun ChangePasswordScreen(
    uiState: PasswordChangeUiState,
    onBack: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmationChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onIssueShown: () -> Unit,
    onChangeAnnounced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val doneMessage = stringResource(R.string.password_change_done)
    val issueMessage = uiState.issue?.takeUnless { uiState.isPasswordIssue || uiState.isConfirmationIssue }
        ?.let { stringResource(it.messageResource()) }
    LaunchedEffect(issueMessage) {
        if (issueMessage != null) {
            snackbarHostState.showSnackbar(issueMessage)
            onIssueShown()
        }
    }
    LaunchedEffect(uiState.isChanged) {
        if (uiState.isChanged) {
            snackbarHostState.showSnackbar(doneMessage)
            onChangeAnnounced()
        }
    }

    TitledScreen(
        title = stringResource(R.string.account_change_password),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            Text(
                text = stringResource(R.string.account_change_password_help, MIN_PASSWORD_LENGTH),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PasswordForm(uiState, onPasswordChanged, onConfirmationChanged, onSaveClicked)
        }
    }
}
