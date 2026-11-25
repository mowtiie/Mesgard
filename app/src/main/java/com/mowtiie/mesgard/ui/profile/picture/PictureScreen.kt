package com.mowtiie.mesgard.ui.profile.picture

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.ui.common.AvatarImage
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen

private val PagePadding = 24.dp
private val Spacing = 16.dp
private val CurrentAvatarSize = 120.dp
private val EditorSide = 300.dp

@Composable
internal fun PictureScreen(
    viewModel: PictureViewModel,
    texts: PictureTexts,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            viewModel.onDoneShown()
            onBack()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }
    TitledScreen(
        title = stringResource(texts.title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { if (uiState.photo != null) viewModel.onCropCancelled() else onBack() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScrollingPage(padding) {
            if (uiState.isLoading || uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val photo = uiState.photo
            if (photo != null) {
                CropStage(photo, uiState.isWorking, viewModel::onSaveClicked, viewModel::onCropCancelled)
            } else {
                uiState.rules?.let { rules ->
                    CurrentPicture(rules, uiState.displayName, texts) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (rules.canRemove) {
                        OutlinedButton(
                            onClick = viewModel::onRemoveClicked,
                            enabled = !uiState.isWorking,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                        ) { Text(stringResource(R.string.profile_picture_remove)) }
                    }
                }
            }
        }
    }
    if (uiState.showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onRemoveDismissed,
            title = { Text(stringResource(texts.removeTitle)) },
            text = { Text(stringResource(texts.removeMessage)) },
            confirmButton = {
                TextButton(onClick = viewModel::onRemoveConfirmed) {
                    Text(stringResource(R.string.profile_picture_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRemoveDismissed) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CurrentPicture(rules: PictureRules, displayName: String, texts: PictureTexts, onChoose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing),
    ) {
        AvatarImage(
            ref = rules.ref,
            name = displayName,
            size = CurrentAvatarSize,
        )
        Text(
            text = rules.explanation(texts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (rules.canSet) {
            Button(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text(
                    text = stringResource(R.string.profile_picture_choose),
                    modifier = Modifier.padding(start = Spacing / 2),
                )
            }
        }
    }
}

@Composable
private fun PictureRules.explanation(texts: PictureTexts): String {
    val blockedUntil = blockedUntilEpochMillis
    return when {
        blockedUntil != null -> stringResource(
            R.string.profile_picture_blocked,
            DateUtils.formatDateTime(LocalContext.current, blockedUntil, DateUtils.FORMAT_SHOW_DATE),
        )
        !canSet -> stringResource(texts.notAllowed)
        else -> stringResource(texts.hint)
    }
}

@Composable
private fun CropStage(photo: PreparedPhoto, isWorking: Boolean, onSave: (CropSquare) -> Unit, onCancel: () -> Unit) {
    var crop by remember(photo) { mutableStateOf<CropSquare?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing),
    ) {
        CropEditor(
            photo = photo,
            side = EditorSide,
            description = stringResource(R.string.profile_picture_crop_description),
            onCropChanged = { crop = it },
        )
        Text(
            text = stringResource(R.string.profile_picture_crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { crop?.let(onSave) },
            enabled = crop != null && !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.profile_picture_save)) }
        TextButton(onClick = onCancel, enabled = !isWorking) { Text(stringResource(R.string.cancel)) }
    }
}

private fun PictureError.messageResource(): Int = when (this) {
    PictureError.NETWORK_UNAVAILABLE -> R.string.profile_picture_error_network
    PictureError.NOT_ALLOWED -> R.string.profile_picture_error_not_allowed
    PictureError.UNREADABLE -> R.string.profile_picture_error_unreadable
    PictureError.UNKNOWN -> R.string.profile_picture_error_unknown
}
