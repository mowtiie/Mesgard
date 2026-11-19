package com.mowtiie.mesgard.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.AuthHeader
import com.mowtiie.mesgard.ui.common.CenteredForm
import com.mowtiie.mesgard.ui.theme.MesgardTheme

@Composable
internal fun MfaCodeRoute(modifier: Modifier = Modifier, viewModel: MfaCodeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MfaCodeScreen(
        uiState = uiState,
        onCodeChanged = viewModel::onCodeChanged,
        onSubmitClicked = viewModel::onSubmitClicked,
        onSignOutClicked = viewModel::onSignOutClicked,
        modifier = modifier,
    )
}

@Composable
internal fun MfaCodeScreen(
    uiState: MfaCodeUiState,
    onCodeChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val codeFocus = remember { FocusRequester() }
    LaunchedEffect(uiState.isVerifying) {
        if (!uiState.isVerifying) codeFocus.requestFocus()
    }
    CenteredForm(
        modifier = modifier,
        offlineMessage = R.string.mfa_offline_banner,
    ) {
        AuthHeader(R.string.mfa_code_title)
        Text(
            text = stringResource(R.string.mfa_code_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MfaCodeField(
            code = uiState.code,
            issue = uiState.issue,
            enabled = !uiState.isVerifying,
            onChange = onCodeChanged,
            onDone = onSubmitClicked,
            focusRequester = codeFocus,
        )
        SubmitButton(
            label = R.string.mfa_verify_button,
            isBusy = uiState.isVerifying,
            enabled = uiState.canSubmit,
            onClick = onSubmitClicked,
        )
        TextButton(onClick = onSignOutClicked, enabled = !uiState.isVerifying) {
            Text(stringResource(R.string.sign_out_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MfaCodeScreenPreview() {
    MesgardTheme {
        MfaCodeScreen(
            uiState = MfaCodeUiState(code = "123", issue = MfaIssue.INVALID_CODE),
            onCodeChanged = {},
            onSubmitClicked = {},
            onSignOutClicked = {},
        )
    }
}
