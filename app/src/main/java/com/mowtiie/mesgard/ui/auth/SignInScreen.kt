package com.mowtiie.mesgard.ui.auth

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.ui.common.AuthHeader
import com.mowtiie.mesgard.ui.common.CenteredForm
import com.mowtiie.mesgard.ui.common.ServerCard
import com.mowtiie.mesgard.ui.connectivity.ConnectionBanner
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus
import com.mowtiie.mesgard.ui.server.ServerStatusNotice
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val FieldSpacing = 16.dp
private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp

@Composable
internal fun SignInRoute(
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignInScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSignInClicked = viewModel::onSignInClicked,
        onErrorShown = viewModel::onErrorShown,
        onChangeServerClicked = onChangeServer,
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
    onChangeServerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isForgotPasswordShown by rememberSaveable { mutableStateOf(false) }
    SaveCredentialsOnSuccess(uiState)
    val snackbarMessage = uiState.error?.takeUnless { it.isFieldError }?.let { stringResource(it.messageResource()) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage, duration = SnackbarDuration.Long)
            onErrorShown()
        }
    }

    CenteredForm(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        banner = {
            ConnectionBanner(status = LocalConnectionStatus.current, offlineMessage = R.string.sign_in_offline_banner)
        },
    ) {
        SignInForm(
            uiState = uiState,
            onEmailChanged = onEmailChanged,
            onPasswordChanged = onPasswordChanged,
            onSignInClicked = onSignInClicked,
            onChangeServerClicked = onChangeServerClicked,
            onForgotPasswordClicked = { isForgotPasswordShown = true },
        )
    }
    if (isForgotPasswordShown) ForgotPasswordSheet(onDismiss = { isForgotPasswordShown = false })
}

/**
 * Tells the password manager the form was submitted, so it can offer to save the credentials. The root replaces this
 * screen once the person is signed in, so leaving while a sign in is in flight means it worked. Turning the device and
 * closing the app also leave the screen, and must not count.
 */
@Composable
private fun SaveCredentialsOnSuccess(uiState: SignInUiState) {
    val autofillManager = LocalAutofillManager.current
    val activity = LocalActivity.current
    val latestState by rememberUpdatedState(uiState)
    DisposableEffect(Unit) {
        onDispose {
            val isLeaving = activity?.isChangingConfigurations != true && activity?.isFinishing != true
            if (latestState.isSubmitting && isLeaving) autofillManager?.commit()
        }
    }
}

/** The error appears under the password field. */
@Composable
private fun ColumnScope.SignInForm(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onChangeServerClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
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
    AuthHeader(R.string.sign_in_title)
    ServerStatusNotice(forAdministrator = false)
    if (uiState.serverHost.isNotBlank()) {
        ServerCard(uiState.serverHost, onChangeServerClicked, enabled = !uiState.isSubmitting)
    }
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
    TextButton(onClick = onForgotPasswordClicked, modifier = Modifier.align(Alignment.End)) {
        Text(stringResource(R.string.sign_in_forgot_password))
    }
    SignInButton(uiState, submit)
    SignInHelp()
}

@Composable
private fun SignInButton(uiState: SignInUiState, onClick: () -> Unit) {
    val progressDescription = stringResource(R.string.sign_in_in_progress)
    Button(
        onClick = onClick,
        enabled = uiState.canSubmit,
        modifier = Modifier.fillMaxWidth().semantics {
            liveRegion = LiveRegionMode.Polite
            if (uiState.isSubmitting) stateDescription = progressDescription
        },
    ) {
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
}

@Composable
private fun SignInHelp() {
    Text(
        text = stringResource(R.string.sign_in_help),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = FieldSpacing),
    )
}

internal val SignInError.isFieldError: Boolean
    get() = this == SignInError.INVALID_CREDENTIALS

@StringRes
internal fun SignInError.messageResource(): Int = when (this) {
    SignInError.INVALID_CREDENTIALS -> R.string.sign_in_error_invalid_credentials
    SignInError.ACCOUNT_DISABLED -> R.string.sign_in_error_account_disabled
    SignInError.TOO_MANY_ATTEMPTS -> R.string.sign_in_error_too_many_attempts
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
            onChangeServerClicked = {},
        )
    }
}
