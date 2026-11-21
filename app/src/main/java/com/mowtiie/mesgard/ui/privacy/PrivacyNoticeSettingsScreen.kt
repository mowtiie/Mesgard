package com.mowtiie.mesgard.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

@Composable
internal fun PrivacyNoticeSettingsScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacyNoticeSettingsViewModel = hiltViewModel(),
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
        title = stringResource(R.string.privacy_notice_settings_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            if (uiState.isLoading || uiState.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.privacy_notice_settings_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinkField(
                value = uiState.noticeUrl,
                label = R.string.privacy_notice_settings_link,
                isValid = uiState.isNoticeUrlValid,
                onChange = viewModel::onNoticeUrlChanged,
            )
            LinkField(
                value = uiState.childNoticeUrl,
                label = R.string.privacy_notice_settings_child_link,
                isValid = uiState.isChildNoticeUrlValid,
                onChange = viewModel::onChildNoticeUrlChanged,
            )
            Button(onClick = viewModel::onSaveClicked, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_reviewers_save))
            }
        }
    }
}

@Composable
private fun LinkField(value: String, label: Int, isValid: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = !isValid,
        supportingText = {
            val hint = R.string.privacy_notice_settings_link_hint
            val error = R.string.privacy_notice_settings_link_error
            Text(stringResource(if (isValid) hint else error))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
}
