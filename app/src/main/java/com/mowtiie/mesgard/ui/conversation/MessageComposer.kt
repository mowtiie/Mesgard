package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val ComposerPadding = 8.dp
private val ComposerSpacing = 8.dp
private const val COMPOSER_MAX_LINES = 5

@Composable
internal fun MessageComposer(
    draft: String,
    canSend: Boolean,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(ComposerPadding),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            label = { Text(stringResource(R.string.message_label)) },
            maxLines = COMPOSER_MAX_LINES,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(ComposerSpacing))
        FilledIconButton(onClick = onSendClicked, enabled = canSend) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send_message),
            )
        }
    }
}

@Composable
internal fun ReadOnlyNotice(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Text(
            text = stringResource(R.string.announcement_read_only),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(ComposerPadding * 2),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageComposerPreview() {
    MesgardTheme {
        MessageComposer(draft = "Hello", canSend = true, onDraftChanged = {}, onSendClicked = {})
    }
}
