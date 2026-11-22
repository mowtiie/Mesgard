package com.mowtiie.mesgard.ui.admin.groups

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.GroupFileProblem
import com.mowtiie.mesgard.domain.admin.GroupImportRow
import com.mowtiie.mesgard.domain.admin.GroupRowProblem
import com.mowtiie.mesgard.domain.model.GroupImportResult
import com.mowtiie.mesgard.domain.model.GroupImportRowResult
import com.mowtiie.mesgard.ui.admin.ImportMessage
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.SectionHeader
import com.mowtiie.mesgard.ui.common.TitledScreen

private const val TEMPLATE_FILE_NAME = "mesgard-groups-template.csv"
private val CSV_TYPES = arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel")
private const val MAX_ROWS_SHOWN = 20
private val ContentPadding = 16.dp

@Composable
internal fun ImportGroupsScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportGroupsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFileChosen(it.toString()) }
    }
    val saveTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onTemplateDestinationChosen(it.toString()) }
    }
    val savedMessage = stringResource(R.string.import_saved)
    val saveFailedMessage = stringResource(R.string.import_save_failed)
    LaunchedEffect(uiState.message) {
        val message = when (uiState.message) {
            ImportMessage.SAVED -> savedMessage
            ImportMessage.SAVE_FAILED -> saveFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(R.string.import_groups_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScrollingPage(padding) {
            when (uiState.stage) {
                ImportGroupsStage.START -> StartStage(
                    fileProblem = uiState.fileProblem,
                    onChooseFile = { chooseFile.launch(CSV_TYPES) },
                    onSaveTemplate = { saveTemplate.launch(TEMPLATE_FILE_NAME) },
                )
                ImportGroupsStage.PREVIEW -> PreviewStage(
                    uiState = uiState,
                    onImport = viewModel::onImportClicked,
                    onChooseAnotherFile = viewModel::onChooseAnotherFile,
                )
                ImportGroupsStage.IMPORTING -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ImportGroupsStage.DONE -> uiState.result?.let { DoneStage(it, onFinish = onBack) }
            }
        }
    }
}

@Composable
private fun StartStage(fileProblem: GroupFileProblem?, onChooseFile: () -> Unit, onSaveTemplate: () -> Unit) {
    Column(modifier = Modifier.padding(ContentPadding), verticalArrangement = Arrangement.spacedBy(ContentPadding)) {
        Text(
            text = stringResource(R.string.import_groups_help),
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
private fun PreviewStage(uiState: ImportGroupsUiState, onImport: () -> Unit, onChooseAnotherFile: () -> Unit) {
    SectionHeader(R.string.import_groups_ready)
    Text(
        text = pluralStringResource(R.plurals.import_groups_group_count, uiState.groupCount, uiState.groupCount) +
            ", " + pluralStringResource(R.plurals.import_groups_people_count, uiState.peopleCount, uiState.peopleCount),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = ContentPadding),
    )
    if (uiState.problemRows.isNotEmpty()) {
        SectionHeader(R.string.import_groups_problems)
        uiState.problemRows.take(MAX_ROWS_SHOWN).forEach { ProblemRow(it) }
        if (uiState.problemRows.size > MAX_ROWS_SHOWN) {
            Text(
                text = stringResource(R.string.import_groups_more, uiState.problemRows.size - MAX_ROWS_SHOWN),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ContentPadding),
            )
        }
    }
    Column(
        modifier = Modifier.padding(ContentPadding),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ContentPadding),
    ) {
        Button(onClick = onImport, enabled = uiState.readyRows.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_groups_button))
        }
        OutlinedButton(onClick = onChooseAnotherFile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_choose_another))
        }
    }
}

@Composable
private fun ProblemRow(row: GroupImportRow) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.import_groups_line, row.line, row.group)) },
        supportingContent = { Text(stringResource((row.problem ?: return@ListItem).messageResource())) },
    )
}

@Composable
private fun DoneStage(result: GroupImportResult, onFinish: () -> Unit) {
    SectionHeader(R.string.import_groups_done)
    Text(
        text = pluralStringResource(R.plurals.import_groups_created, result.createdGroups, result.createdGroups) +
            ", " + pluralStringResource(R.plurals.import_groups_added, result.addedMembers, result.addedMembers),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = ContentPadding),
    )
    if (result.rowsToReview.isNotEmpty()) {
        SectionHeader(R.string.import_groups_review)
        result.rowsToReview.take(MAX_ROWS_SHOWN).forEach { ReviewRow(it) }
    }
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(ContentPadding)) {
        Text(stringResource(R.string.report_done))
    }
}

@Composable
private fun ReviewRow(row: GroupImportRowResult) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.import_groups_row, row.line)) },
        supportingContent = {
            Text(row.error ?: stringResource(R.string.import_groups_not_found))
        },
    )
}

@StringRes
private fun GroupFileProblem.messageResource(): Int = when (this) {
    GroupFileProblem.EMPTY -> R.string.import_groups_problem_empty
    GroupFileProblem.NO_GROUP_COLUMN -> R.string.import_groups_problem_no_group_column
    GroupFileProblem.TOO_MANY_ROWS -> R.string.import_groups_problem_too_many
    GroupFileProblem.UNREADABLE -> R.string.import_groups_problem_unreadable
}

@StringRes
private fun GroupRowProblem.messageResource(): Int = when (this) {
    GroupRowProblem.NO_GROUP_NAME -> R.string.import_groups_row_no_name
    GroupRowProblem.GROUP_NAME_TOO_LONG -> R.string.import_groups_row_name_too_long
    GroupRowProblem.UNKNOWN_KIND -> R.string.import_groups_row_unknown_kind
    GroupRowProblem.INVALID_EMAIL -> R.string.import_groups_row_invalid_email
    GroupRowProblem.UNKNOWN_ROLE -> R.string.import_groups_row_unknown_role
}
