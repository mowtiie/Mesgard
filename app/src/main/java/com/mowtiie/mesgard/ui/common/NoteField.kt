package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private const val DEFAULT_MIN_LINES = 2

/**
 * A multi line field with a short label and its requirement under it, as the Material 3 text field guidance asks:
 * the label names the field, the supporting text says what it takes (optional, required, a minimum length) and the
 * counter on the right shows how much of the limit is used.
 */
@Composable
internal fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    helper: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    minLines: Int = DEFAULT_MIN_LINES,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(stringResource(label)) },
        supportingText = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = helper, modifier = Modifier.weight(1f))
                Text(text = "${value.length} / $maxLength", modifier = Modifier.widthIn(min = 0.dp))
            }
        },
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
