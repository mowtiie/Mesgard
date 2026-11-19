package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.layout.Column
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
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.model.PostingPolicy

private const val MAX_GROUP_NAME_LENGTH = 200

/** A new group has a name, and can limit posting to moderators and include everyone, like an announcements group. */
@Composable
internal fun NewGroupDialog(isWorking: Boolean, onConfirm: (String, GroupSettings) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var moderatorsOnly by rememberSaveable { mutableStateOf(false) }
    var includesEveryone by rememberSaveable { mutableStateOf(false) }
    val settings = GroupSettings(
        postingPolicy = if (moderatorsOnly) PostingPolicy.MODERATORS else PostingPolicy.EVERYONE,
        includesEveryone = includesEveryone,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.groups_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_GROUP_NAME_LENGTH) name = it },
                    label = { Text(stringResource(R.string.groups_name_label)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                )
                GroupSettingsRows(
                    settings = settings,
                    enabled = !isWorking,
                    onChanged = {
                        moderatorsOnly = it.postingPolicy == PostingPolicy.MODERATORS
                        includesEveryone = it.includesEveryone
                    },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, settings) }, enabled = name.isNotBlank() && !isWorking) {
                Text(stringResource(R.string.groups_create_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
