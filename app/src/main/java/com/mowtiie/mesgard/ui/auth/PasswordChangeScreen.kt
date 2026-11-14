package com.mowtiie.mesgard.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.auth.MIN_PASSWORD_LENGTH
import com.mowtiie.mesgard.ui.common.CenteredForm

private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp

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
        NewPasswordField(
            value = uiState.password,
            label = R.string.password_change_new_label,
            issue = uiState.issue?.takeIf { uiState.isPasswordIssue },
            enabled = !uiState.isSaving,
            imeAction = ImeAction.Next,
            onChange = onPasswordChanged,
        )
        NewPasswordField(
            value = uiState.confirmation,
            label = R.string.password_change_confirm_label,
            issue = uiState.issue?.takeIf { uiState.isConfirmationIssue },
            enabled = !uiState.isSaving,
            imeAction = ImeAction.Done,
            onChange = onConfirmationChanged,
            onDone = onSaveClicked,
        )
        Button(onClick = onSaveClicked, enabled = uiState.canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressSize),
                    color = LocalContentColor.current,
                    trackColor = Color.Transparent,
                    strokeWidth = ProgressStrokeWidth,
                )
            } else {
                Text(stringResource(R.string.password_change_save))
            }
        }
        TextButton(onClick = onSignOutClicked, enabled = !uiState.isSaving) {
            Text(stringResource(R.string.sign_out_button))
        }
    }
}

@Composable
private fun NewPasswordField(
    value: String,
    @StringRes label: Int,
    issue: PasswordChangeIssue?,
    enabled: Boolean,
    imeAction: ImeAction,
    onChange: (String) -> Unit,
    onDone: () -> Unit = {},
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    val message = issue?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        enabled = enabled,
        isError = message != null,
        trailingIcon = { PasswordVisibilityToggle(visible = isVisible, onClick = { isVisible = !isVisible }) },
        supportingText = message?.let { text -> { Text(text) } },
        modifier = Modifier.fillMaxWidth().semantics {
            contentType = ContentType.NewPassword
            message?.let { error(it) }
        },
    )
}

@StringRes
private fun PasswordChangeIssue.messageResource(): Int = when (this) {
    PasswordChangeIssue.TOO_SHORT -> R.string.password_change_error_too_short
    PasswordChangeIssue.MISMATCH -> R.string.password_change_error_mismatch
    PasswordChangeIssue.WEAK_PASSWORD -> R.string.password_change_error_weak
    PasswordChangeIssue.SAME_PASSWORD -> R.string.password_change_error_same
    PasswordChangeIssue.NETWORK_UNAVAILABLE -> R.string.sign_in_error_network_unavailable
    PasswordChangeIssue.UNKNOWN -> R.string.password_change_error_unknown
}
