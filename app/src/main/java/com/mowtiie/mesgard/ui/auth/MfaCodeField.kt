package com.mowtiie.mesgard.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.mowtiie.mesgard.R

/** Six digits from the authenticator app. The form submits itself when the sixth digit arrives. */
@Composable
internal fun MfaCodeField(
    code: String,
    issue: MfaIssue?,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    val errorMessage = issue?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = code,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.mfa_code_label)) },
        textStyle = TextStyle.Default.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        enabled = enabled,
        isError = issue != null,
        supportingText = errorMessage?.let { message ->
            { Text(text = message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { errorMessage?.let { error(it) } },
    )
}

@StringRes
internal fun MfaIssue.messageResource(): Int = when (this) {
    MfaIssue.INVALID_CODE -> R.string.mfa_error_invalid_code
    MfaIssue.TOO_MANY_ATTEMPTS -> R.string.mfa_error_too_many_attempts
    MfaIssue.NETWORK_UNAVAILABLE -> R.string.mfa_error_network_unavailable
    MfaIssue.UNKNOWN -> R.string.mfa_error_unknown
}
