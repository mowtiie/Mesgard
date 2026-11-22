package com.mowtiie.mesgard.ui.admin.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private val ContentPadding = 16.dp

@Composable
internal fun GroupCreationScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupCreationViewModel = hiltViewModel(),
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
        title = stringResource(R.string.group_creation_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScrollingPage(padding) {
            if (uiState.isLoading || uiState.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.group_creation_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
            RadioGroup(
                options = GroupCreationPolicy.entries.map {
                    it to RadioLabel(it.labelResource(), it.summaryResource())
                },
                selected = uiState.policy,
                onSelected = viewModel::onSelected,
                enabled = !uiState.isLoading && !uiState.isSaving,
            )
            uiState.limit?.let {
                Text(
                    text = stringResource(R.string.group_creation_limit_note, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ContentPadding),
                )
            }
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ContentPadding),
            ) {
                Text(stringResource(R.string.report_reviewers_save))
            }
        }
    }
}

@StringRes
internal fun GroupCreationPolicy.labelResource(): Int = when (this) {
    GroupCreationPolicy.ADMINS_ONLY -> R.string.group_creation_admins_only
    GroupCreationPolicy.STAFF -> R.string.group_creation_staff
    GroupCreationPolicy.EVERYONE -> R.string.group_creation_everyone
}

@StringRes
private fun GroupCreationPolicy.summaryResource(): Int = when (this) {
    GroupCreationPolicy.ADMINS_ONLY -> R.string.group_creation_admins_only_summary
    GroupCreationPolicy.STAFF -> R.string.group_creation_staff_summary
    GroupCreationPolicy.EVERYONE -> R.string.group_creation_everyone_summary
}
