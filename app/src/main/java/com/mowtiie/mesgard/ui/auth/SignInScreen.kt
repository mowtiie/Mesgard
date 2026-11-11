package com.mowtiie.mesgard.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.ui.connectivity.ConnectionBanner
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val FormMaxWidth = 400.dp
private val ContentPadding = 24.dp
private val FieldSpacing = 16.dp
private val TopSpacing = 56.dp
private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp

@Composable
internal fun SignInRoute(modifier: Modifier = Modifier, viewModel: SignInViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignInScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSignInClicked = viewModel::onSignInClicked,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@Composable
internal fun SignInScreen(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.error?.takeUnless { it.isFieldError }?.let { stringResource(it.messageResource()) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ConnectionBanner(status = LocalConnectionStatus.current, offlineMessage = R.string.sign_in_offline_banner)
            Column(
                modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(TopSpacing))
                SignInForm(uiState, onEmailChanged, onPasswordChanged, onSignInClicked)
            }
        }
    }
}

/**
 * The form is anchored to the top and scrolls, so the keyboard, landscape and large fonts never hide the button.
 * The error replaces the supporting text under the password, so nothing moves when it appears.
 */
@Composable
private fun SignInForm(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
) {
    val fieldError = uiState.error?.takeIf { it.isFieldError }
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    val submit = {
        if (uiState.canSubmit) {
            focusManager.clearFocus()
            onSignInClicked()
        }
    }
    LaunchedEffect(fieldError, uiState.isSubmitting) {
        if (fieldError != null && !uiState.isSubmitting) passwordFocus.requestFocus()
    }
    Column(
        modifier = Modifier.widthIn(max = FormMaxWidth).padding(horizontal = ContentPadding),
        verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    ) {
        BrandHeader()
        EmailField(
            email = uiState.email,
            enabled = !uiState.isSubmitting,
            hasError = fieldError != null,
            onChange = onEmailChanged,
            onNext = { passwordFocus.requestFocus() },
        )
        PasswordField(
            password = uiState.password,
            enabled = !uiState.isSubmitting,
            error = fieldError,
            onChange = onPasswordChanged,
            onDone = submit,
            focusRequester = passwordFocus,
        )
        Button(onClick = submit, enabled = uiState.canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressSize),
                    color = LocalContentColor.current,
                    trackColor = Color.Transparent,
                    strokeWidth = ProgressStrokeWidth,
                )
            } else {
                Text(stringResource(R.string.sign_in_button))
            }
        }
        SignInHelp(uiState.serverHost)
    }
}

@Composable
private fun BrandHeader() {
    Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.sign_in_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignInHelp(serverHost: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = FieldSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2),
    ) {
        Text(
            text = stringResource(R.string.sign_in_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (serverHost.isNotBlank()) {
            Text(
                text = stringResource(R.string.sign_in_server, serverHost),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal val SignInError.isFieldError: Boolean
    get() = this == SignInError.INVALID_CREDENTIALS

@StringRes
internal fun SignInError.messageResource(): Int = when (this) {
    SignInError.INVALID_CREDENTIALS -> R.string.sign_in_error_invalid_credentials
    SignInError.NETWORK_UNAVAILABLE -> R.string.sign_in_error_network_unavailable
    SignInError.UNKNOWN -> R.string.sign_in_error_unknown
}

@Preview(showBackground = true)
@Composable
private fun SignInScreenPreview() {
    MesgardTheme {
        SignInScreen(
            uiState = SignInUiState(
                email = "student1@rnhs.test",
                password = "password",
                error = SignInError.INVALID_CREDENTIALS,
                serverHost = "mesgard.school.example",
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onSignInClicked = {},
            onErrorShown = {},
        )
    }
}
