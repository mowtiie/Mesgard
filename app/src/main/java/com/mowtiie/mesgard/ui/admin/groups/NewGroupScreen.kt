package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.NavigationChevron
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.SectionHeader
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private const val MAX_GROUP_NAME_LENGTH = 200

/** Someone who is not an administrator makes clubs and other flat groups, not classes or sections. */
private fun kindsFor(state: NewGroupUiState): List<GroupKind> =
    if (state.isAdmin) GroupKind.entries else listOf(GroupKind.CLUB, GroupKind.OTHER)
private val FieldPadding = 16.dp

/** A page, not a sheet: a group has a name, a kind, a place in the tree and who can post. */
@Composable
internal fun NewGroupScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewGroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isCreated) { if (uiState.isCreated) onBack() }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }
    var isPickerShown by rememberSaveable { mutableStateOf(false) }

    TitledScreen(
        title = stringResource(R.string.groups_create_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScrollingPage(padding) {
            if (uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            NewGroupForm(uiState, viewModel, onChooseParent = { isPickerShown = true })
        }
    }
    if (isPickerShown) {
        ParentPickerSheet(
            groups = uiState.groups,
            selectedId = uiState.parentId,
            excludedIds = emptySet(),
            onSelected = {
                viewModel.onParentChosen(it)
                isPickerShown = false
            },
            onDismiss = { isPickerShown = false },
        )
    }
}

@Composable
private fun NewGroupForm(uiState: NewGroupUiState, viewModel: NewGroupViewModel, onChooseParent: () -> Unit) {
    OutlinedTextField(
        value = uiState.name,
        onValueChange = { if (it.length <= MAX_GROUP_NAME_LENGTH) viewModel.onNameChanged(it) },
        label = { Text(stringResource(R.string.groups_name_label)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        singleLine = true,
        enabled = !uiState.isWorking,
        modifier = Modifier.fillMaxWidth().padding(FieldPadding),
    )
    SectionHeader(R.string.group_kind_title)
    RadioGroup(
        options = kindsFor(uiState).map { it to RadioLabel(it.labelResource()) },
        selected = uiState.kind,
        onSelected = viewModel::onKindChanged,
        enabled = !uiState.isWorking,
    )
    if (uiState.isAdmin) {
        SectionHeader(R.string.group_parent_title)
        ListItem(
            modifier = Modifier.clickable(role = Role.Button, enabled = !uiState.isWorking, onClick = onChooseParent),
            headlineContent = { Text(uiState.parentName ?: stringResource(R.string.group_parent_top)) },
            trailingContent = { NavigationChevron() },
        )
        GroupSettingsRows(uiState.settings, enabled = !uiState.isWorking, onChanged = viewModel::onSettingsChanged)
    } else {
        Text(
            text = stringResource(R.string.group_create_member_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(FieldPadding),
        )
    }
    Column(modifier = Modifier.padding(FieldPadding)) {
        Button(onClick = viewModel::onCreateClicked, enabled = uiState.canCreate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.groups_create_confirm))
        }
    }
}
