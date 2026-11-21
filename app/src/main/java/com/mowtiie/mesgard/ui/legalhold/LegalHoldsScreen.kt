package com.mowtiie.mesgard.ui.legalhold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.HOLD_RELEASE_NOTE_MIN_LENGTH
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.LegalHold
import com.mowtiie.mesgard.domain.model.LegalHoldScope
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.EmptyState
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.common.NoteField
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.reports.rememberDateTimeFormatter

private const val EMPTY_STATE_HEIGHT_FRACTION = 0.7f
private val ContentPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegalHoldsScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LegalHoldsViewModel = hiltViewModel(),
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
    var isPlacingOrganizationHold by rememberSaveable { mutableStateOf(false) }
    if (isPlacingOrganizationHold) {
        PlaceHoldSheet(
            target = HoldTarget.Organization,
            onDismiss = { isPlacingOrganizationHold = false },
            onPlaced = {
                isPlacingOrganizationHold = false
                viewModel.onRefresh()
            },
        )
    }
    uiState.holdToRelease?.let { hold ->
        ReleaseSheet(hold, onConfirm = viewModel::onReleaseConfirmed, onDismiss = viewModel::onReleaseDismissed)
    }
    TitledScreen(
        title = stringResource(R.string.legal_holds_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        onRefresh = if (uiState.isLoading) null else viewModel::onRefresh,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isWorking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.holds.isEmpty(),
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                HoldList(uiState, onRelease = viewModel::onReleaseRequested, onHoldOrganization = {
                    isPlacingOrganizationHold = true
                })
            }
        }
    }
}

@Composable
private fun HoldList(uiState: LegalHoldsUiState, onRelease: (LegalHold) -> Unit, onHoldOrganization: () -> Unit) {
    val formatTime = rememberDateTimeFormatter()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "intro") {
            Column(modifier = Modifier.padding(ContentPadding)) {
                Text(
                    text = stringResource(R.string.legal_holds_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onHoldOrganization,
                    modifier = Modifier.fillMaxWidth().padding(top = ContentPadding),
                ) {
                    Text(stringResource(R.string.legal_hold_place_organization_button))
                }
            }
        }
        if (uiState.showEmptyMessage) {
            item(key = "empty") {
                EmptyState(
                    R.drawable.illustration_audit_log,
                    R.string.legal_holds_empty,
                    R.string.legal_holds_empty_hint,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_STATE_HEIGHT_FRACTION),
                )
            }
        }
        items(uiState.holds, key = { it.id }) { hold ->
            ListItem(
                overlineContent = { Text(stringResource(hold.scope.labelResource())) },
                headlineContent = { Text(hold.targetName()) },
                supportingContent = {
                    val line =
                        stringResource(
                            R.string.report_row_summary,
                            hold.placedByName,
                            formatTime(hold.placedAtEpochMillis),
                        )
                    Text(listOf(hold.reason, line).joinToString("\n"))
                },
                trailingContent = {
                    TextButton(onClick = { onRelease(hold) }) { Text(stringResource(R.string.legal_hold_release)) }
                },
            )
        }
    }
}

/** Releasing ends the protection, so a note says why. */
@Composable
private fun ReleaseSheet(hold: LegalHold, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = stringResource(R.string.legal_hold_release_title),
        confirmLabel = stringResource(R.string.legal_hold_release),
        onConfirm = { onConfirm(note) },
        onDismiss = onDismiss,
        confirmEnabled = note.trim().length >= HOLD_RELEASE_NOTE_MIN_LENGTH,
    ) {
        Text(
            text = stringResource(R.string.legal_hold_release_text, hold.targetName()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NoteField(
            value = note,
            onValueChange = { note = it },
            label = R.string.legal_hold_release_note_label,
            helper = stringResource(R.string.legal_hold_release_note_helper),
            maxLength = REPORT_NOTE_MAX_LENGTH,
        )
    }
}

@Composable
private fun LegalHold.targetName(): String = when (scope) {
    LegalHoldScope.PERSON -> personName ?: stringResource(R.string.report_reporter_anonymous)
    LegalHoldScope.GROUP -> groupName ?: stringResource(R.string.legal_hold_group_gone)
    LegalHoldScope.ORGANIZATION -> stringResource(R.string.legal_hold_scope_organization_name)
}

private fun LegalHoldScope.labelResource(): Int = when (this) {
    LegalHoldScope.PERSON -> R.string.legal_hold_scope_person
    LegalHoldScope.GROUP -> R.string.legal_hold_scope_group
    LegalHoldScope.ORGANIZATION -> R.string.legal_hold_scope_organization
}
