package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val ComposerPadding = 8.dp
private val ChipSpacing = 8.dp
private val ChipMaxWidth = 200.dp
private val RemoveIconSize = 18.dp
private const val COMPOSER_MAX_LINES = 5

@Composable
internal fun MessageComposer(
    draft: String,
    attachments: List<PickedAttachment>,
    canSend: Boolean,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onAttachmentsPicked: (List<PickedAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickers = rememberAttachmentPickers(onAttachmentsPicked)
    Column(modifier = modifier.fillMaxWidth().padding(ComposerPadding)) {
        if (attachments.isNotEmpty()) PickedAttachments(attachments, onAttachmentRemoved)
        Row(verticalAlignment = Alignment.Bottom) {
            AttachButton(pickers)
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                label = { Text(stringResource(R.string.message_label)) },
                maxLines = COMPOSER_MAX_LINES,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = onSendClicked, enabled = canSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_message),
                )
            }
        }
    }
}

@Composable
private fun AttachButton(pickers: AttachmentPickers) {
    var isMenuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { isMenuOpen = true }) {
        Icon(painterResource(R.drawable.ic_attach_file), contentDescription = stringResource(R.string.attach))
    }
    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.attach_photo)) },
            onClick = {
                isMenuOpen = false
                pickers.pickPhotos()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.attach_file)) },
            onClick = {
                isMenuOpen = false
                pickers.pickFiles()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickedAttachments(attachments: List<PickedAttachment>, onRemoved: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(bottom = ComposerPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        items(attachments, key = { it.uri }) { attachment ->
            InputChip(
                modifier = Modifier.widthIn(max = ChipMaxWidth),
                selected = false,
                onClick = { onRemoved(attachment.uri) },
                label = { Text(attachment.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_attachment, attachment.name),
                        modifier = Modifier.size(RemoveIconSize),
                    )
                },
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
        MessageComposer(
            draft = "Hello",
            attachments = listOf(PickedAttachment("content://a", "report.pdf")),
            canSend = true,
            onDraftChanged = {},
            onSendClicked = {},
            onAttachmentsPicked = {},
            onAttachmentRemoved = {},
        )
    }
}
