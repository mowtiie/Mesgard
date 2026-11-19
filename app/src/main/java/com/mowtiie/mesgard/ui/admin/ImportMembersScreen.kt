package com.mowtiie.mesgard.ui.admin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.admin.formatCredentials
import com.mowtiie.mesgard.ui.common.TitledScreen
import kotlinx.coroutines.launch

private val CSV_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "application/vnd.ms-excel",
    "text/plain",
)
private const val TEMPLATE_FILE_NAME = "mesgard-roster-template.csv"
private const val CREDENTIALS_FILE_NAME = "mesgard-temporary-passwords.csv"

@Composable
internal fun ImportMembersScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportMembersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var isLeaveDialogShown by rememberSaveable { mutableStateOf(false) }

    val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFileChosen(it.toString()) }
    }
    val saveTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onTemplateDestinationChosen(it.toString()) }
    }
    val saveCredentials = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onCredentialsDestinationChosen(it.toString()) }
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

    val leave = { if (uiState.needsLeaveWarning) isLeaveDialogShown = true else onBack() }
    BackHandler(enabled = uiState.stage != ImportStage.START) { leave() }

    TitledScreen(
        title = stringResource(R.string.import_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = leave) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState.stage) {
                ImportStage.START -> StartStage(
                    fileProblem = uiState.fileProblem,
                    onChooseFile = { chooseFile.launch(CSV_TYPES) },
                    onSaveTemplate = { saveTemplate.launch(TEMPLATE_FILE_NAME) },
                )
                ImportStage.PREVIEW -> PreviewStage(
                    ready = uiState.readyEntries,
                    problems = uiState.problemEntries,
                    onImport = viewModel::onImportClicked,
                    onChooseAnotherFile = viewModel::onChooseAnotherFile,
                )
                ImportStage.IMPORTING -> ImportingStage(uiState.importedCount, uiState.readyEntries.size)
                ImportStage.DONE -> DoneStage(
                    state = uiState,
                    onCopy = {
                        scope.launch {
                            clipboard.setClipEntry(sensitiveClip(formatCredentials(uiState.credentials)))
                            viewModel.onCredentialsCopied()
                        }
                    },
                    onSave = { saveCredentials.launch(CREDENTIALS_FILE_NAME) },
                    onFinish = leave,
                )
            }
        }
    }
    if (isLeaveDialogShown) {
        AlertDialog(
            onDismissRequest = { isLeaveDialogShown = false },
            title = { Text(stringResource(R.string.import_leave_title)) },
            text = { Text(stringResource(R.string.import_leave_message)) },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.import_leave_confirm)) } },
            dismissButton = {
                TextButton(onClick = { isLeaveDialogShown = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
