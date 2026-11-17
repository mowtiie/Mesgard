package com.mowtiie.mesgard.ui.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.ui.admin.sensitiveClip
import com.mowtiie.mesgard.ui.common.AuthHeader
import com.mowtiie.mesgard.ui.common.CenteredForm
import com.mowtiie.mesgard.ui.common.SecureWindowEffect
import com.mowtiie.mesgard.ui.connectivity.ConnectionBanner
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus
import com.mowtiie.mesgard.ui.theme.MesgardTheme
import kotlinx.coroutines.launch

private val KeyPadding = 16.dp
private val KeySpacing = 8.dp

@Composable
internal fun MfaEnrollmentRoute(modifier: Modifier = Modifier, viewModel: MfaEnrollmentViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MfaEnrollmentScreen(
        uiState = uiState,
        onCodeChanged = viewModel::onCodeChanged,
        onConfirmClicked = viewModel::onConfirmClicked,
        onRetryClicked = viewModel::onRetryClicked,
        onSignOutClicked = viewModel::onSignOutClicked,
        modifier = modifier,
    )
}

@Composable
internal fun MfaEnrollmentScreen(
    uiState: MfaEnrollmentUiState,
    onCodeChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureWindowEffect()
    val snackbarHostState = remember { SnackbarHostState() }
    CenteredForm(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        banner = {
            ConnectionBanner(status = LocalConnectionStatus.current, offlineMessage = R.string.mfa_offline_banner)
        },
    ) {
        AuthHeader(R.string.mfa_enroll_title)
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.enrollment == null -> StartFailure(uiState, onRetryClicked)
            else -> EnrollmentSteps(uiState, uiState.enrollment, snackbarHostState, onCodeChanged, onConfirmClicked)
        }
        TextButton(onClick = onSignOutClicked, enabled = !uiState.isVerifying) {
            Text(stringResource(R.string.sign_out_button))
        }
    }
}

@Composable
private fun StartFailure(uiState: MfaEnrollmentUiState, onRetryClicked: () -> Unit) {
    Text(
        text = stringResource((uiState.issue ?: MfaIssue.UNKNOWN).messageResource()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    OutlinedButton(onClick = onRetryClicked) { Text(stringResource(R.string.mfa_enroll_retry)) }
}

@Composable
private fun EnrollmentSteps(
    uiState: MfaEnrollmentUiState,
    enrollment: MfaEnrollment,
    snackbarHostState: SnackbarHostState,
    onCodeChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
) {
    Text(
        text = stringResource(R.string.mfa_enroll_help),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    SetupKey(enrollment, snackbarHostState)
    Text(
        text = stringResource(R.string.mfa_enroll_confirm_help),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    MfaCodeField(
        code = uiState.code,
        issue = uiState.issue,
        enabled = !uiState.isVerifying,
        onChange = onCodeChanged,
        onDone = onConfirmClicked,
    )
    SubmitButton(
        label = R.string.mfa_enroll_confirm_button,
        isBusy = uiState.isVerifying,
        enabled = uiState.canSubmit,
        onClick = onConfirmClicked,
    )
}

/** The key is what the person types into the authenticator app when no app opens the link. */
@Composable
private fun SetupKey(enrollment: MfaEnrollment, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val noAuthenticatorMessage = stringResource(R.string.mfa_enroll_no_authenticator)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(KeyPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KeySpacing),
        ) {
            Text(
                text = stringResource(R.string.mfa_enroll_key_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatSetupKey(enrollment.secret),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { scope.launch { clipboard.setClipEntry(sensitiveClip(enrollment.secret)) } }) {
                Text(stringResource(R.string.mfa_enroll_copy_key))
            }
        }
    }
    OutlinedButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, enrollment.uri.toUri())
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(noAuthenticatorMessage) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.mfa_enroll_open_authenticator))
    }
}

@Preview(showBackground = true)
@Composable
private fun MfaEnrollmentScreenPreview() {
    MesgardTheme {
        MfaEnrollmentScreen(
            uiState = MfaEnrollmentUiState(
                enrollment = MfaEnrollment("factor", "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP", "otpauth://totp/x"),
                isLoading = false,
                code = "123",
            ),
            onCodeChanged = {},
            onConfirmClicked = {},
            onRetryClicked = {},
            onSignOutClicked = {},
        )
    }
}
