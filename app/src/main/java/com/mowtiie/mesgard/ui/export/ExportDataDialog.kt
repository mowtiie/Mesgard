package com.mowtiie.mesgard.ui.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.repository.ExportFile
import com.mowtiie.mesgard.ui.admin.messageResource
import java.io.File

private const val EXPORT_MIME_TYPE = "application/json"

/** Explains what an export holds, creates it, then offers it to share. It is recorded in the audit log. */
@Composable
internal fun ExportDataDialog(
    userId: String,
    personName: String,
    onDismiss: () -> Unit,
    viewModel: DataExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val close = {
        viewModel.onDialogClosed()
        onDismiss()
    }
    val file = uiState.file
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) close() },
        title = { Text(stringResource(if (file == null) R.string.export_title else R.string.export_ready_title)) },
        text = {
            val error = uiState.error
            when {
                uiState.isExporting -> CircularProgressIndicator()
                file != null -> Text(stringResource(R.string.export_ready_text, file.fileName))
                error != null -> Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error)
                else -> Text(stringResource(R.string.export_text, personName))
            }
        },
        confirmButton = {
            if (file != null) {
                TextButton(onClick = { if (shareExport(context, file)) close() }) {
                    Text(stringResource(R.string.export_share))
                }
            } else {
                TextButton(
                    onClick = { viewModel.onExportClicked(userId, personName) },
                    enabled = !uiState.isExporting,
                ) {
                    Text(stringResource(R.string.export_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = close, enabled = !uiState.isExporting) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Offers the file to another app. Returns false when no app can take it. */
internal fun shareExport(context: Context, file: ExportFile): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", File(file.path))
    val intent = Intent(Intent.ACTION_SEND)
        .setType(EXPORT_MIME_TYPE)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(Intent.createChooser(intent, null))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
