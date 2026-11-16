package com.mowtiie.mesgard.ui.account

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Signing out erases the data on the device, including messages that have not been sent, so it asks first. */
@Composable
internal fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sign_out_dialog_title)) },
        text = { Text(stringResource(R.string.sign_out_dialog_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.sign_out_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
