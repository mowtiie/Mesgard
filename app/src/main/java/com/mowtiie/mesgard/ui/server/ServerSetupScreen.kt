package com.mowtiie.mesgard.ui.server

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.AuthHeader
import com.mowtiie.mesgard.ui.common.CenteredForm
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val FieldSpacing = 16.dp
private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp
private const val ACCESS_KEY_MAX_LINES = 4

@Composable
internal fun ServerSetupRoute(
    prefill: ServerSetupPrefill?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(prefill) { prefill?.let(viewModel::onPrefill) }
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            viewModel.onFinishedHandled()
            onFinished()
        }
    }
    ServerSetupScreen(
        uiState = uiState,
        onAddressChanged = viewModel::onAddressChanged,
        onAccessKeyChanged = viewModel::onAccessKeyChanged,
        onConnectClicked = viewModel::onConnectClicked,
        onCancelClicked = onFinished,
        modifier = modifier,
    )
}

@Composable
internal fun ServerSetupScreen(
    uiState: ServerSetupUiState,
    onAddressChanged: (String) -> Unit,
    onAccessKeyChanged: (String) -> Unit,
    onConnectClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredForm(modifier = modifier) {
        SetupHeader(uiState.isFromLink)
        AddressField(uiState, onAddressChanged)
        AccessKeyField(uiState, onAccessKeyChanged, onConnectClicked)
        if (uiState.error == ServerSetupError.SIGN_OUT_FIRST) SignOutFirstNotice()
        ConnectButton(uiState, onConnectClicked)
        if (uiState.canCancel) {
            TextButton(onClick = onCancelClicked, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun SetupHeader(isFromLink: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeader(R.string.server_setup_title)
        Text(
            text = stringResource(if (isFromLink) R.string.server_setup_link_warning else R.string.server_setup_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddressField(uiState: ServerSetupUiState, onChange: (String) -> Unit) {
    val message = uiState.error?.takeIf { uiState.isAddressError }?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = uiState.address,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.server_setup_address_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        singleLine = true,
        enabled = !uiState.isChecking,
        isError = message != null,
        supportingText = { FieldSupport(message, R.string.server_setup_address_hint) },
        modifier = Modifier.fillMaxWidth().semantics { message?.let { error(it) } },
    )
}

@Composable
private fun AccessKeyField(uiState: ServerSetupUiState, onChange: (String) -> Unit, onDone: () -> Unit) {
    val message = uiState.error?.takeIf { uiState.isAccessKeyError }?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = uiState.accessKey,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.server_setup_key_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        maxLines = ACCESS_KEY_MAX_LINES,
        enabled = !uiState.isChecking,
        isError = message != null,
        supportingText = { FieldSupport(message, R.string.server_setup_key_hint) },
        modifier = Modifier.fillMaxWidth().semantics { message?.let { error(it) } },
    )
}

/** The hint explains the field, and an error takes its place so the layout stays still. */
@Composable
private fun FieldSupport(message: String?, @StringRes hint: Int) {
    Text(
        text = message ?: stringResource(hint),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun SignOutFirstNotice() {
    Text(
        text = stringResource(ServerSetupError.SIGN_OUT_FIRST.messageResource()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ConnectButton(uiState: ServerSetupUiState, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !uiState.isChecking && uiState.address.isNotBlank() && uiState.accessKey.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = LocalContentColor.current,
                trackColor = Color.Transparent,
                strokeWidth = ProgressStrokeWidth,
            )
        } else {
            Text(stringResource(R.string.server_setup_connect))
        }
    }
}

@StringRes
internal fun ServerSetupError.messageResource(): Int = when (this) {
    ServerSetupError.ADDRESS_EMPTY -> R.string.server_setup_error_address_empty
    ServerSetupError.ADDRESS_INVALID -> R.string.server_setup_error_address_invalid
    ServerSetupError.ADDRESS_NOT_SECURE -> R.string.server_setup_error_address_not_secure
    ServerSetupError.KEY_EMPTY -> R.string.server_setup_error_key_empty
    ServerSetupError.KEY_MALFORMED -> R.string.server_setup_error_key_malformed
    ServerSetupError.KEY_SECRET -> R.string.server_setup_error_key_secret
    ServerSetupError.SIGN_OUT_FIRST -> R.string.server_setup_error_sign_out_first
    ServerSetupError.UNREACHABLE -> R.string.server_setup_error_unreachable
    ServerSetupError.KEY_REJECTED -> R.string.server_setup_error_key_rejected
    ServerSetupError.NOT_A_SERVER -> R.string.server_setup_error_not_a_server
}

@Preview(showBackground = true)
@Composable
private fun ServerSetupScreenPreview() {
    MesgardTheme {
        ServerSetupScreen(
            uiState = ServerSetupUiState(
                address = "chat.school.example",
                accessKey = "sb_publishable_example",
                canCancel = true,
                error = ServerSetupError.UNREACHABLE,
            ),
            onAddressChanged = {},
            onAccessKeyChanged = {},
            onConnectClicked = {},
            onCancelClicked = {},
        )
    }
}
