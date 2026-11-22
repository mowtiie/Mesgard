package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.RosterEntry
import com.mowtiie.mesgard.domain.admin.RosterFileProblem
import com.mowtiie.mesgard.domain.admin.RosterProblem
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.SkipReason

private val ScreenPadding = 16.dp

@Composable
internal fun StartStage(fileProblem: RosterFileProblem?, onChooseFile: () -> Unit, onSaveTemplate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenPadding),
    ) {
        Text(stringResource(R.string.import_help), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.import_help_columns),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        fileProblem?.let {
            Text(
                text = stringResource(it.messageResource()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_choose_file))
        }
        OutlinedButton(onClick = onSaveTemplate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_save_template))
        }
    }
}

@Composable
internal fun PreviewStage(
    ready: List<RosterEntry>,
    problems: List<RosterEntry>,
    onImport: () -> Unit,
    onChooseAnotherFile: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = pluralStringResource(R.plurals.import_ready_count, ready.size, ready.size),
                style = MaterialTheme.typography.titleMedium,
            )
            if (problems.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.import_problem_count, problems.size, problems.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = onImport, enabled = ready.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.import_button, ready.size, ready.size))
            }
            TextButton(onClick = onChooseAnotherFile, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_choose_another))
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(problems, key = { "problem:${it.line}" }) { entry ->
                ListItem(
                    headlineContent = { Text(entry.email.ifBlank { stringResource(R.string.import_no_email) }) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.import_line_problem,
                                entry.line,
                                stringResource(entry.problem.messageResource()),
                            ),
                        )
                    },
                )
            }
            items(ready, key = { "ready:${it.line}" }) { entry ->
                ListItem(
                    headlineContent = { Text(entry.displayName) },
                    supportingContent = { Text(entry.email) },
                )
            }
        }
    }
}

@Composable
internal fun ImportingStage(done: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenPadding, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.import_progress, done, total), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = {
            if (total ==
                0
            ) {
                0f
            } else {
                done.toFloat() / total
            }
        }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun DoneStage(state: ImportMembersUiState, onCopy: () -> Unit, onSave: () -> Unit, onFinish: () -> Unit) {
    val credentials = state.credentials
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(ScreenPadding)) {
        item(key = "summary") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = ScreenPadding),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.import_created_count, credentials.size, credentials.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.skipped.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.import_skipped_count,
                            state.skipped.size,
                            state.skipped.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.interruptedBy?.let {
                    Text(
                        text = stringResource(it.interruptedResource()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (credentials.isNotEmpty()) {
                    Text(stringResource(R.string.import_passwords_warning), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_save_file))
                    }
                    OutlinedButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_copy_all))
                    }
                }
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.members_password_done))
                }
            }
        }
        items(credentials, key = { "created:${it.email}" }) { credential ->
            ListItem(
                headlineContent = { Text(credential.displayName) },
                supportingContent = { Text(credential.email) },
                trailingContent = { Text(credential.temporaryPassword, fontFamily = FontFamily.Monospace) },
            )
        }
        items(state.skipped, key = { "skipped:${it.email}" }) { skipped ->
            ListItem(
                headlineContent = { Text(skipped.email) },
                supportingContent = { Text(stringResource(skipped.reason.messageResource())) },
            )
        }
    }
}

@StringRes
private fun RosterFileProblem.messageResource(): Int = when (this) {
    RosterFileProblem.EMPTY -> R.string.import_file_empty
    RosterFileProblem.NO_EMAIL_COLUMN -> R.string.import_file_no_email_column
    RosterFileProblem.TOO_MANY_ROWS -> R.string.import_file_too_many_rows
    RosterFileProblem.UNREADABLE -> R.string.import_file_unreadable
}

@StringRes
private fun RosterProblem?.messageResource(): Int = when (this) {
    RosterProblem.INVALID_EMAIL -> R.string.import_problem_invalid_email
    RosterProblem.DUPLICATE_EMAIL -> R.string.import_problem_duplicate
    RosterProblem.UNKNOWN_ROLE -> R.string.import_problem_unknown_role
    RosterProblem.NAME_TOO_LONG -> R.string.import_problem_name_too_long
    null -> R.string.admin_error_unknown
}

@StringRes
private fun SkipReason.messageResource(): Int = when (this) {
    SkipReason.EMAIL_TAKEN -> R.string.members_error_email_taken
    SkipReason.DUPLICATE -> R.string.import_problem_duplicate
    SkipReason.INVALID_EMAIL -> R.string.import_problem_invalid_email
    SkipReason.INVALID_ROLE -> R.string.import_problem_unknown_role
    SkipReason.INVALID_NAME -> R.string.import_problem_name_too_long
    SkipReason.FAILED -> R.string.import_problem_failed
}

@StringRes
private fun AdminError.interruptedResource(): Int = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> R.string.import_interrupted_network
    AdminError.NOT_ALLOWED -> R.string.admin_error_not_allowed
    AdminError.NOT_A_MEMBER, AdminError.LAST_ADMIN, AdminError.NOT_ERASABLE, AdminError.LEGAL_HOLD,
    AdminError.GROUP_NOT_EMPTY, AdminError.GROUP_TREE_INVALID,
    AdminError.GROUP_LIMIT,
    AdminError.UNKNOWN,
    -> R.string.import_interrupted_unknown
}
