package com.mowtiie.mesgard.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SignInError

/** Next moves to the password and the content type lets a password manager fill the field. */
@Composable
internal fun EmailField(
    email: String,
    enabled: Boolean,
    hasError: Boolean,
    onChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.sign_in_email_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        singleLine = true,
        enabled = enabled,
        isError = hasError,
        trailingIcon = { if (hasError) ErrorIcon() },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.EmailAddress + ContentType.Username },
    )
}

/**
 * The supporting line is always there: it explains the field, and the error takes its place so the layout stays
 * still. Done submits the form.
 */
@Composable
internal fun PasswordField(
    password: String,
    enabled: Boolean,
    error: SignInError?,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    val errorMessage = error?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = password,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.sign_in_password_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        trailingIcon = { PasswordVisibilityToggle(visible = isVisible, onClick = { isVisible = !isVisible }) },
        supportingText = {
            Text(
                text = errorMessage ?: stringResource(R.string.sign_in_password_hint),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics {
                contentType = ContentType.Password
                errorMessage?.let { error(it) }
            },
    )
}

@Composable
private fun ErrorIcon() {
    Icon(painter = painterResource(R.drawable.ic_error), contentDescription = null)
}

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
            contentDescription = stringResource(
                if (visible) R.string.sign_in_hide_password else R.string.sign_in_show_password,
            ),
        )
    }
}
