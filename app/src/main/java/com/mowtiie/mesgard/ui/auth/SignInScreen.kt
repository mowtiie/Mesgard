package com.mowtiie.mesgard.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val ContentPadding = 24.dp
private val FieldSpacing = 16.dp
private val ProgressSize = 20.dp

@Composable
internal fun SignInRoute(modifier: Modifier = Modifier, viewModel: SignInViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignInScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSignInClicked = viewModel::onSignInClicked,
        modifier = modifier,
    )
}

@Composable
internal fun SignInScreen(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(FieldSpacing, Alignment.CenterVertically),
        ) {
            Text(text = stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                label = { Text(stringResource(R.string.sign_in_email_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                label = { Text(stringResource(R.string.sign_in_password_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.error?.let { error ->
                Text(
                    text = stringResource(error.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onSignInClicked, enabled = uiState.canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(ProgressSize), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.sign_in_button))
                }
            }
        }
    }
}

@StringRes
private fun SignInError.messageResource(): Int = when (this) {
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
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onSignInClicked = {},
        )
    }
}
