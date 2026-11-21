package com.mowtiie.mesgard.ui.reports

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

@Composable
internal fun ReportReviewersScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportReviewersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onBack() }
    TitledScreen(
        title = stringResource(R.string.report_reviewers_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading || uiState.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val intro = if (uiState.adminsReview) {
                R.string.report_reviewers_admins_review
            } else {
                R.string.report_reviewers_intro
            }
            Text(
                text = stringResource(intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.candidates, key = { it.userId }) { person ->
                    CandidateRow(person, person.userId in uiState.selectedIds, onToggle = {
                        viewModel.onToggled(person.userId)
                    })
                }
            }
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(ContentPadding),
            ) {
                Text(stringResource(R.string.report_reviewers_save))
            }
        }
    }
}

@Composable
private fun CandidateRow(person: Contact, isSelected: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() }),
        leadingContent = { InitialAvatar(person.displayName) },
        headlineContent = { Text(person.displayName) },
        supportingContent = {
            Text(stringResource(if (person.role == OrgRole.ORG_ADMIN) R.string.role_org_admin else R.string.role_staff))
        },
        trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) },
    )
}
