package com.mowtiie.mesgard.ui.account

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Turning it off lowers the protection of the account, so it asks first. */
@Composable
internal fun DisableMfaDialog(isWorking: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_mfa_disable_title)) },
        text = { Text(stringResource(R.string.account_mfa_disable_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                Text(stringResource(R.string.account_mfa_disable_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text(stringResource(R.string.cancel)) }
        },
    )
}
