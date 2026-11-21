package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val SheetPadding = 24.dp
private val SheetSpacing = 16.dp
private val ButtonSpacing = 8.dp

/**
 * A bottom sheet for one task with one or two fields: a title, the content, and Cancel with the confirming action.
 * Dialogs stay for a short question with no field. The sheet opens fully and moves above the keyboard, so a text
 * field is never covered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = SheetPadding).padding(bottom = SheetPadding),
            verticalArrangement = Arrangement.spacedBy(SheetSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonSpacing, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = dismissEnabled) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
            }
        }
    }
}
