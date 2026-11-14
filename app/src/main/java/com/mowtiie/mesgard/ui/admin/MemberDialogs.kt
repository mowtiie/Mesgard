package com.mowtiie.mesgard.ui.admin

import android.content.ClipData
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.labelResource
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel
import kotlinx.coroutines.launch

private const val SENSITIVE_CONTENT_EXTRA = "android.content.extra.IS_SENSITIVE"
internal val AssignableRoles = listOf(OrgRole.STUDENT, OrgRole.STAFF, OrgRole.ORG_ADMIN)

@Composable
internal fun MembersDialogs(
    dialog: MembersDialog?,
    isWorking: Boolean,
    onRoleSelected: (OrgRole) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is MembersDialog.ChangeRole -> ChangeRoleDialog(dialog, isWorking, onRoleSelected, onConfirm, onDismiss)
        is MembersDialog.ConfirmResetPassword -> ConfirmDialog(
            title = R.string.members_reset_title,
            message = stringResource(R.string.members_reset_message, dialog.member.displayName),
            confirmLabel = R.string.members_reset_confirm,
            isWorking = isWorking,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is MembersDialog.ConfirmRemove -> ConfirmDialog(
            title = R.string.members_remove_title,
            message = stringResource(R.string.members_remove_message, dialog.member.displayName),
            confirmLabel = R.string.members_remove_confirm,
            isWorking = isWorking,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is MembersDialog.TemporaryPassword -> TemporaryPasswordDialog(dialog.memberName, dialog.password, onDismiss)
        null -> Unit
    }
}

@Composable
private fun ChangeRoleDialog(
    dialog: MembersDialog.ChangeRole,
    isWorking: Boolean,
    onRoleSelected: (OrgRole) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.members_role_title, dialog.member.displayName)) },
        text = {
            RadioGroup(
                options = AssignableRoles.map { it to RadioLabel(it.labelResource()) },
                selected = dialog.selected,
                onSelected = onRoleSelected,
                enabled = !isWorking,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking && dialog.selected != dialog.member.role) {
                Text(stringResource(R.string.members_role_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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
                    scope.launch {
                        val clip = ClipData.newPlainText(null, password).apply {
                            description.extras = PersistableBundle().apply { putBoolean(SENSITIVE_CONTENT_EXTRA, true) }
                        }
                        clipboard.setClipEntry(ClipEntry(clip))
                    }
                },
            ) { Text(stringResource(R.string.members_password_copy)) }
        },
    )
}
