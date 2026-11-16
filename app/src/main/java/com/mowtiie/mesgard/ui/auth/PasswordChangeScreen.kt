package com.mowtiie.mesgard.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.auth.MIN_PASSWORD_LENGTH
import com.mowtiie.mesgard.ui.common.CenteredForm

@Composable
internal fun PasswordChangeRoute(modifier: Modifier = Modifier, viewModel: PasswordChangeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PasswordChangeScreen(
        uiState = uiState,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmationChanged = viewModel::onConfirmationChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onSignOutClicked = viewModel::onSignOutClicked,
        onIssueShown = viewModel::onIssueShown,
        modifier = modifier,
    )
}

@Composable
internal fun PasswordChangeScreen(
    uiState: PasswordChangeUiState,
    onPasswordChanged: (String) -> Unit,
    onConfirmationChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onIssueShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.issue?.takeUnless { uiState.isPasswordIssue || uiState.isConfirmationIssue }
        ?.let { stringResource(it.messageResource()) }
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onIssueShown()
        }
    }

    CenteredForm(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Text(
            text = stringResource(R.string.password_change_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.password_change_help, MIN_PASSWORD_LENGTH),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PasswordForm(uiState, onPasswordChanged, onConfirmationChanged, onSaveClicked)
        TextButton(onClick = onSignOutClicked, enabled = !uiState.isSaving) {
            Text(stringResource(R.string.sign_out_button))
        }
    }
}
