package com.mowtiie.mesgard.ui.retention

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

@Composable
internal fun RetentionScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RetentionViewModel = hiltViewModel(),
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
        title = stringResource(R.string.retention_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (uiState.isLoading || uiState.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.retention_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
            Column(modifier = Modifier.selectableGroup()) {
                uiState.options.forEach { days ->
                    ListItem(
                        modifier = Modifier.selectable(
                            selected = days == uiState.selectedDays,
                            enabled = !uiState.isLoading && !uiState.isSaving,
                            role = Role.RadioButton,
                            onClick = { viewModel.onSelected(days) },
                        ),
                        leadingContent = { RadioButton(selected = days == uiState.selectedDays, onClick = null) },
                        headlineContent = { Text(periodLabel(days)) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.retention_job_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
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

private const val DAYS_IN_YEAR = 365

@Composable
private fun periodLabel(days: Int?): String = when {
    days == null -> stringResource(R.string.retention_keep)
    days % DAYS_IN_YEAR == 0 -> {
        val years = days / DAYS_IN_YEAR
        pluralStringResource(R.plurals.retention_years, years, years)
    }
    else -> pluralStringResource(R.plurals.retention_days, days, days)
}
