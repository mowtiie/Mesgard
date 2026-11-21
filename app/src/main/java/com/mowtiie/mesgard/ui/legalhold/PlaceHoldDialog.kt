package com.mowtiie.mesgard.ui.legalhold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.HOLD_REASON_MIN_LENGTH
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.REPORT_NOTE_MAX_LENGTH
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.NoteField

private val FieldSpacing = 12.dp

/**
 * Says what the hold protects, asks for the reason and places it. A reason is needed so the hold can be explained.
 * Once it is placed the dialog says so, and [onPlaced] is called when the person closes it.
 */
@Composable
internal fun PlaceHoldDialog(
    target: HoldTarget,
    onDismiss: () -> Unit,
    onPlaced: () -> Unit,
    viewModel: PlaceHoldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isPlaced) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.legal_hold_placed_title)) },
            text = { Text(stringResource(R.string.legal_hold_placed_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onPlacedHandled()
                        onPlaced()
                    },
                ) {
                    Text(stringResource(R.string.report_done))
                }
            },
        )
        return
    }
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.legal_hold_place_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
                Text(stringResource(target.explanationResource(), target.displayName()))
                NoteField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = R.string.legal_hold_reason_label,
                    helper = stringResource(R.string.legal_hold_reason_helper, HOLD_REASON_MIN_LENGTH),
                    maxLength = REPORT_NOTE_MAX_LENGTH,
                    enabled = !uiState.isPlacing,
                )
                uiState.error?.let {
                    Text(
                        text = stringResource(it.messageResource()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.onPlaceClicked(target, reason) },
                enabled = !uiState.isPlacing && reason.trim().length >= HOLD_REASON_MIN_LENGTH,
            ) {
                Text(stringResource(R.string.legal_hold_place_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun HoldTarget.explanationResource(): Int = when (this) {
    is HoldTarget.Person -> R.string.legal_hold_place_person
    is HoldTarget.Group -> R.string.legal_hold_place_group
    HoldTarget.Organization -> R.string.legal_hold_place_organization
}

private fun HoldTarget.displayName(): String = when (this) {
    is HoldTarget.Person -> name
    is HoldTarget.Group -> name
    HoldTarget.Organization -> ""
}
