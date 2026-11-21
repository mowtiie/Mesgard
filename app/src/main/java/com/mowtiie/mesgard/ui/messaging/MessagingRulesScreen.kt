package com.mowtiie.mesgard.ui.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.settings.SwitchRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ContentPadding = 16.dp
private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm")

private enum class ClockTarget { START, END }

@Composable
internal fun MessagingRulesScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessagingRulesViewModel = hiltViewModel(),
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
    var clockTarget by rememberSaveable { mutableStateOf<ClockTarget?>(null) }
    val rules = uiState.rules
    clockTarget?.let { target ->
        val initial = (if (target == ClockTarget.START) rules.quietStart else rules.quietEnd) ?: LocalTime.MIDNIGHT
        ClockDialog(
            initial = initial,
            onConfirm = {
                if (target == ClockTarget.START) viewModel.onQuietStartChanged(it) else viewModel.onQuietEndChanged(it)
                clockTarget = null
            },
            onDismiss = { clockTarget = null },
        )
    }
    TitledScreen(
        title = stringResource(R.string.messaging_rules_title),
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
            SwitchRow(
                title = R.string.messaging_rules_block_title,
                summary = R.string.messaging_rules_block_summary,
                checked = rules.blockStaffStudentDirectMessages,
                onCheckedChange = viewModel::onBlockChanged,
            )
            SwitchRow(
                title = R.string.messaging_rules_quiet_title,
                summary = R.string.messaging_rules_quiet_summary,
                checked = rules.hasQuietHours,
                onCheckedChange = viewModel::onQuietHoursChanged,
            )
            val start = rules.quietStart
            val end = rules.quietEnd
            if (start != null && end != null) {
                ClockRow(R.string.messaging_rules_quiet_from, start) { clockTarget = ClockTarget.START }
                ClockRow(R.string.messaging_rules_quiet_until, end) { clockTarget = ClockTarget.END }
                Text(
                    text = stringResource(R.string.messaging_rules_time_zone, rules.timeZone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ContentPadding),
                )
            }
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().padding(ContentPadding),
            ) {
                Text(stringResource(R.string.report_reviewers_save))
            }
        }
    }
}

@Composable
private fun ClockRow(label: Int, time: LocalTime, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        overlineContent = { Text(stringResource(label)) },
        headlineContent = { Text(time.format(ClockFormat)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.messaging_rules_time_set))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
