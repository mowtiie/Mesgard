package com.mowtiie.mesgard.ui.account

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.USERNAME_CHANGE_DAYS
import com.mowtiie.mesgard.domain.model.USERNAME_MAX_LENGTH
import com.mowtiie.mesgard.domain.model.USERNAME_MIN_LENGTH
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.common.NavigationChevron
import com.mowtiie.mesgard.ui.common.SectionHeader

/** The username in each organization. A person who may change it opens a sheet with one field. */
@Composable
internal fun UsernameSection(
    memberships: List<AccountMembership>,
    modifier: Modifier = Modifier,
    viewModel: UsernameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(memberships) { viewModel.onMembershipsChanged(memberships) }
    if (uiState.rows.isNotEmpty()) {
        SectionHeader(R.string.account_username_section, modifier)
        val showsOrganization = uiState.rows.size > 1
        uiState.rows.forEach { row ->
            UsernameRow(row, showsOrganization, onClick = { viewModel.onEditRequested(row) })
        }
    }
    uiState.edit?.let { edit ->
        FormSheet(
            title = stringResource(R.string.account_username_change_title),
            confirmLabel = stringResource(R.string.account_username_save),
            onConfirm = viewModel::onSaveClicked,
            onDismiss = viewModel::onEditDismissed,
            confirmEnabled = edit.text.isNotBlank() && !edit.isSaving,
            dismissEnabled = !edit.isSaving,
        ) {
            UsernameField(edit, viewModel::onTextChanged, viewModel::onSaveClicked)
        }
    }
}

@Composable
private fun UsernameRow(row: UsernameRow, showsOrganization: Boolean, onClick: () -> Unit) {
    val clickable = if (row.canChange) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    ListItem(
        modifier = clickable,
        leadingContent = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
        overlineContent = if (showsOrganization) ({ Text(row.organizationName) }) else null,
        headlineContent = { Text(stringResource(R.string.contact_username, row.rules.username)) },
        supportingContent = { Text(row.supportingText()) },
        trailingContent = if (row.canChange) ({ NavigationChevron() }) else null,
    )
}

@Composable
private fun UsernameRow.supportingText(): String {
    val nextChange = rules.nextChangeAtEpochMillis
    return when {
        !rules.policyAllowsChange -> stringResource(R.string.account_username_not_allowed)
        nextChange != null -> stringResource(
            R.string.account_username_wait,
            DateUtils.formatDateTime(LocalContext.current, nextChange, DateUtils.FORMAT_SHOW_DATE),
        )
        else -> stringResource(R.string.account_username_hint)
    }
}

@Composable
private fun UsernameField(edit: UsernameEdit, onTextChanged: (String) -> Unit, onSave: () -> Unit) {
    val error = edit.error?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = edit.text,
        onValueChange = onTextChanged,
        label = { Text(stringResource(R.string.account_username_label)) },
        prefix = { Text("@") },
        supportingText = {
            Text(
                error ?: stringResource(
                    R.string.account_username_rules,
                    USERNAME_MIN_LENGTH,
                    USERNAME_MAX_LENGTH,
                    USERNAME_CHANGE_DAYS,
                ),
            )
        },
        isError = error != null,
        singleLine = true,
        enabled = !edit.isSaving,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSave() }),
        modifier = Modifier.fillMaxWidth(),
    )
}
