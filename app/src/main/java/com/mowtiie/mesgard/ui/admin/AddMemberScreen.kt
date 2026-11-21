package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.labelResource
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private val ContentPadding = 16.dp
private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp

@Composable
internal fun AddMemberScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMemberViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddMemberScreen(
        uiState = uiState,
        onBack = onBack,
        onEmailChanged = viewModel::onEmailChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onRoleSelected = viewModel::onRoleSelected,
        onAddClicked = viewModel::onAddClicked,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
    uiState.created?.let {
        TemporaryPasswordDialog(memberName = it.displayName, password = it.temporaryPassword) {
            viewModel.onCreatedHandled()
            onBack()
        }
    }
}

@Composable
internal fun AddMemberScreen(
    uiState: AddMemberUiState,
    onBack: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onRoleSelected: (OrgRole) -> Unit,
    onAddClicked: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.error?.takeUnless { uiState.isEmailError || uiState.isNameError }
        ?.let { stringResource(it.messageResource()) }
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(R.string.members_add),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            EmailField(uiState, onEmailChanged)
            NameField(uiState, onDisplayNameChanged, onAddClicked)
            Text(
                text = stringResource(R.string.members_role_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            RadioGroup(
                options = AssignableRoles.map { it to RadioLabel(it.labelResource()) },
                selected = uiState.role,
                onSelected = onRoleSelected,
                enabled = !uiState.isSaving,
            )
            Text(
                text = stringResource(R.string.members_add_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AddButton(uiState, onAddClicked)
        }
    }
}

@Composable
private fun EmailField(uiState: AddMemberUiState, onChange: (String) -> Unit) {
    val message = uiState.error?.takeIf { uiState.isEmailError }?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = uiState.email,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.members_email_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        singleLine = true,
        enabled = !uiState.isSaving,
        isError = message != null,
        supportingText = message?.let { text -> { Text(text) } },
        modifier = Modifier.fillMaxWidth().semantics { message?.let { error(it) } },
    )
}

@Composable
private fun NameField(uiState: AddMemberUiState, onChange: (String) -> Unit, onDone: () -> Unit) {
    val message = uiState.error?.takeIf { uiState.isNameError }?.let { stringResource(it.messageResource()) }
    OutlinedTextField(
        value = uiState.displayName,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.members_name_label)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        enabled = !uiState.isSaving,
        isError = message != null,
        supportingText = { Text(message ?: stringResource(R.string.members_name_helper)) },
        modifier = Modifier.fillMaxWidth().semantics { message?.let { error(it) } },
    )
}

@Composable
private fun AddButton(uiState: AddMemberUiState, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = uiState.canSubmit, modifier = Modifier.fillMaxWidth()) {
        if (uiState.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = LocalContentColor.current,
                trackColor = Color.Transparent,
                strokeWidth = ProgressStrokeWidth,
            )
        } else {
            Text(stringResource(R.string.members_add_button))
        }
    }
}

@StringRes
private fun AddMemberError.messageResource(): Int = when (this) {
    AddMemberError.EMAIL_INVALID -> R.string.members_error_email_invalid
    AddMemberError.EMAIL_TAKEN -> R.string.members_error_email_taken
    AddMemberError.NAME_INVALID -> R.string.members_error_name_invalid
    AddMemberError.NETWORK_UNAVAILABLE -> R.string.admin_error_network_unavailable
    AddMemberError.NOT_ALLOWED -> R.string.admin_error_not_allowed
    AddMemberError.UNKNOWN -> R.string.admin_error_unknown
}
