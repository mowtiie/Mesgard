package com.mowtiie.mesgard.ui.admin.usernames

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
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private val ContentPadding = 16.dp

@Composable
internal fun UsernamePolicyScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsernamePolicyViewModel = hiltViewModel(),
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
        title = stringResource(R.string.username_policy_title),
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
                text = stringResource(R.string.username_policy_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
            RadioGroup(
                options = UsernamePolicy.entries.map { it to RadioLabel(it.labelResource(), it.summaryResource()) },
                selected = uiState.policy,
                onSelected = viewModel::onSelected,
                enabled = !uiState.isLoading && !uiState.isSaving,
            )
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
internal fun UsernamePolicy.labelResource(): Int = when (this) {
    UsernamePolicy.EVERYONE -> R.string.username_policy_everyone
    UsernamePolicy.STAFF_AND_ADMINS -> R.string.username_policy_staff_and_admins
    UsernamePolicy.NOBODY -> R.string.username_policy_nobody
}

@StringRes
private fun UsernamePolicy.summaryResource(): Int = when (this) {
    UsernamePolicy.EVERYONE -> R.string.username_policy_everyone_summary
    UsernamePolicy.STAFF_AND_ADMINS -> R.string.username_policy_staff_and_admins_summary
    UsernamePolicy.NOBODY -> R.string.username_policy_nobody_summary
}
