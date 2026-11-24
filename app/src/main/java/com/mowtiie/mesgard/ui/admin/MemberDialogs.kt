package com.mowtiie.mesgard.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.common.labelResource
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel
import kotlinx.coroutines.launch

internal val AssignableRoles = listOf(OrgRole.MEMBER, OrgRole.STAFF, OrgRole.ORG_ADMIN)

@Composable
internal fun MembersDialogs(
    dialog: MembersDialog?,
    isWorking: Boolean,
    erasureMode: ErasureMode,
    onRoleSelected: (OrgRole) -> Unit,
    onEraseToggled: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is MembersDialog.ChangeRole -> ChangeRoleSheet(dialog, isWorking, onRoleSelected, onConfirm, onDismiss)
        is MembersDialog.ConfirmResetPassword -> ConfirmDialog(
            title = R.string.members_reset_title,
            message = stringResource(R.string.members_reset_message, dialog.member.displayName),
            confirmLabel = R.string.members_reset_confirm,
            isWorking = isWorking,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is MembersDialog.ConfirmResetMfa -> ConfirmDialog(
            title = R.string.members_reset_mfa_title,
            message = stringResource(R.string.members_reset_mfa_message, dialog.member.displayName),
            confirmLabel = R.string.members_reset_mfa_confirm,
            isWorking = isWorking,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is MembersDialog.ConfirmResetUsername -> ConfirmDialog(
            title = R.string.members_reset_username_title,
            message = stringResource(R.string.members_reset_username_message, dialog.member.displayName),
            confirmLabel = R.string.members_reset_username_confirm,
            isWorking = isWorking,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is MembersDialog.ConfirmRemove ->
            RemoveDialog(dialog, erasureMode, isWorking, onEraseToggled, onConfirm, onDismiss)
        is MembersDialog.TemporaryPassword -> TemporaryPasswordDialog(dialog.memberName, dialog.password, onDismiss)
        null -> Unit
    }
}

@Composable
private fun ChangeRoleSheet(
    dialog: MembersDialog.ChangeRole,
    isWorking: Boolean,
    onRoleSelected: (OrgRole) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormSheet(
        title = stringResource(R.string.members_role_title, dialog.member.displayName),
        confirmLabel = stringResource(R.string.members_role_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = !isWorking && dialog.selected != dialog.member.role,
    ) {
        RadioGroup(
            options = AssignableRoles.map { it to RadioLabel(it.labelResource()) },
            selected = dialog.selected,
            onSelected = onRoleSelected,
            enabled = !isWorking,
        )
    }
}

/** The choice to erase is only there when the server allows it, and it says what this server does. */
@Composable
private fun RemoveDialog(
    dialog: MembersDialog.ConfirmRemove,
    erasureMode: ErasureMode,
    isWorking: Boolean,
    onEraseToggled: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.members_remove_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.members_remove_message, dialog.member.displayName))
                if (erasureMode != ErasureMode.OFF) {
                    EraseChoice(dialog.erase, erasureMode, !isWorking, onEraseToggled)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                Text(
                    stringResource(
                        if (dialog.erase) R.string.members_erase_confirm else R.string.members_remove_confirm,
                    ),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EraseChoice(erase: Boolean, mode: ErasureMode, enabled: Boolean, onToggled: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(
                value = erase,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onToggled,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = erase, onCheckedChange = null, enabled = enabled)
            Text(
                text = stringResource(R.string.members_remove_erase),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (erase) {
            val help = if (mode ==
                ErasureMode.BLANK_MESSAGES
            ) {
                R.string.members_erase_help_blank
            } else {
                R.string.members_erase_help_keep
            }
            Text(
                text = stringResource(help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: Int,
    message: String,
    confirmLabel: Int,
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

/** The password is shown once and can be copied, marked as sensitive so Android hides it from clipboard previews. */
@Composable
internal fun TemporaryPasswordDialog(memberName: String, password: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.members_password_title, memberName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.members_password_message))
                SelectionContainer {
                    Text(
                        text = password,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.members_password_done)) } },
        dismissButton = {
            TextButton(
                onClick = {
                    scope.launch { clipboard.setClipEntry(sensitiveClip(password)) }
                },
            ) { Text(stringResource(R.string.members_password_copy)) }
        },
    )
}
