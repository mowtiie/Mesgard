package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.AvatarImage
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.SearchAction
import com.mowtiie.mesgard.ui.common.SearchAppBar
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.common.avatarRef
import com.mowtiie.mesgard.ui.common.labelResource

private val ButtonPadding = 16.dp

@Composable
internal fun AddGroupMembersScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddGroupMembersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDone) { if (uiState.isDone) onBack() }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }
    var isSearching by rememberSaveable { mutableStateOf(false) }

    TitledScreen(
        title = stringResource(R.string.group_add_members),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = { SearchAction { isSearching = true } },
        searchBar = if (isSearching) {
            {
                SearchAppBar(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChanged,
                    onClose = {
                        isSearching = false
                        viewModel.onQueryChanged("")
                    },
                    placeholder = stringResource(R.string.members_search_placeholder),
                )
            }
        } else {
            null
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            CandidateList(uiState, viewModel::onToggled, modifier = Modifier.weight(1f))
            Button(
                onClick = viewModel::onAddClicked,
                enabled = uiState.canAdd,
                modifier = Modifier.fillMaxWidth().padding(ButtonPadding),
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.group_add_button,
                        uiState.selectedIds.size,
                        uiState.selectedIds.size,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CandidateList(uiState: AddGroupMembersUiState, onToggled: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (uiState.candidates.isEmpty() && !uiState.isLoading) {
            EmptyState(
                R.drawable.illustration_members,
                R.string.group_add_empty,
                R.string.group_add_empty_hint,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.candidates, key = { it.userId }) { contact ->
                val isSelected = contact.userId in uiState.selectedIds
                ListItem(
                    modifier = Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onToggled(contact.userId) },
                    ),
                    leadingContent = { AvatarImage(contact.avatarRef(), contact.displayName) },
                    headlineContent = { Text(contact.displayName) },
                    supportingContent = { Text(stringResource(contact.role.labelResource())) },
                    trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) },
                )
            }
        }
    }
}
