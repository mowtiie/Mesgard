package com.mowtiie.mesgard.ui.admin.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.FormSheet

private const val MAX_GROUP_NAME_LENGTH = 200

/** Asks for the name of a group, to rename one. */
@Composable
internal fun GroupNameSheet(
    @StringRes title: Int,
    @StringRes confirmLabel: Int,
    initialName: String,
    isWorking: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    FormSheet(
        title = stringResource(title),
        confirmLabel = stringResource(confirmLabel),
        onConfirm = { onConfirm(name) },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && !isWorking,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= MAX_GROUP_NAME_LENGTH) name = it },
            label = { Text(stringResource(R.string.groups_name_label)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun GroupConfirmDialog(
    @StringRes title: Int,
    message: String,
    @StringRes confirmLabel: Int,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) { Text(stringResource(confirmLabel)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
