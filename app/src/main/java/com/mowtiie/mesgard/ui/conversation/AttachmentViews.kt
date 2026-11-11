package com.mowtiie.mesgard.ui.conversation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.data.attachment.copyForSharing
import com.mowtiie.mesgard.domain.model.Attachment
import java.io.File

private val ImageWidth = 220.dp
private val ImageHeight = 160.dp
private val FileIconSize = 32.dp
private val ItemSpacing = 4.dp
private val FileRowSpacing = 12.dp
private val ProgressSize = 24.dp

@Composable
internal fun AttachmentList(
    attachments: List<Attachment>,
    downloadingIds: Set<String>,
    onClick: (Attachment) -> Unit,
    onImageShown: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        attachments.forEach { attachment ->
            if (attachment.isImage) {
                ImageAttachment(attachment, onClick, onImageShown)
            } else {
                FileAttachment(attachment, attachment.id in downloadingIds, onClick)
            }
        }
    }
}

@Composable
private fun ImageAttachment(attachment: Attachment, onClick: (Attachment) -> Unit, onShown: (Attachment) -> Unit) {
    LaunchedEffect(attachment.id, attachment.localPath) { onShown(attachment) }
    val description = stringResource(R.string.attachment_photo, attachment.fileName)
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = Modifier.size(ImageWidth, ImageHeight)
            .clip(shape)
            .clickable(enabled = attachment.localPath != null) { onClick(attachment) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val localPath = attachment.localPath
        if (localPath == null) {
            CircularProgressIndicator(modifier = Modifier.size(ProgressSize))
        } else {
            AsyncImage(
                model = File(localPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ImageWidth, ImageHeight),
            )
        }
    }
}

@Composable
private fun FileAttachment(attachment: Attachment, isDownloading: Boolean, onClick: (Attachment) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isDownloading) { onClick(attachment) }
            .padding(vertical = ItemSpacing),
        horizontalArrangement = Arrangement.spacedBy(FileRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isDownloading) {
            CircularProgressIndicator(modifier = Modifier.size(FileIconSize).padding(ItemSpacing))
        } else {
            Icon(painterResource(R.drawable.ic_description), contentDescription = null, Modifier.size(FileIconSize))
        }
        Column {
            Text(text = attachment.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(
                text = Formatter.formatShortFileSize(LocalContext.current, attachment.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Opens the file in another app. Returns false when the file is missing or no app can show it. */
internal fun openAttachment(context: Context, attachment: Attachment): Boolean {
    val source = attachment.localPath?.let(::File)?.takeIf { it.isFile } ?: return false
    val shared = context.copyForSharing(attachment.id, attachment.fileName, source)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", shared)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, attachment.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
