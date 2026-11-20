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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
private const val DEFAULT_IMAGE_RATIO = 220f / 160f
private const val MIN_IMAGE_RATIO = 0.6f
private const val MAX_IMAGE_RATIO = 2f
private const val PHOTOS_PER_ROW = 2
private const val LAST_PHOTO_MAX_RATIO = 2f
private val GridGap = 2.dp
private val FileIconSize = 32.dp
private val ItemSpacing = 4.dp
private val FileRowSpacing = 12.dp
private val ProgressSize = 24.dp
private val MinTouchTarget = 48.dp

@Composable
internal fun AttachmentList(
    attachments: List<Attachment>,
    downloads: AttachmentDownloadsUiState,
    onFileClick: (Attachment) -> Unit,
    onImageClick: (Attachment) -> Unit,
    onImageShown: (Attachment) -> Unit,
    onRetryDownload: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        attachments.forEach { attachment ->
            if (attachment.isImage) {
                ImageAttachment(
                    attachment,
                    attachment.id in downloads.failedIds,
                    onImageClick,
                    onImageShown,
                    onRetryDownload,
                    modifier = Modifier.width(ImageWidth)
                        .aspectRatio(attachment.displayRatio())
                        .clip(MaterialTheme.shapes.medium),
                )
            } else {
                FileAttachment(attachment, attachment.id in downloads.downloadingIds, onFileClick)
            }
        }
    }
}

/** Photos that fill a bubble: one photo with its own shape, two or more in rows of two. */
@Composable
internal fun ImageGrid(
    attachments: List<Attachment>,
    downloads: AttachmentDownloadsUiState,
    onImageClick: (Attachment) -> Unit,
    onImageShown: (Attachment) -> Unit,
    onRetryDownload: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun tile(attachment: Attachment, tileModifier: Modifier) = ImageAttachment(
        attachment,
        attachment.id in downloads.failedIds,
        onImageClick,
        onImageShown,
        onRetryDownload,
        tileModifier,
    )

    if (attachments.size == 1) {
        val photo = attachments.single()
        tile(photo, modifier.fillMaxWidth().aspectRatio(photo.displayRatio()))
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GridGap)) {
        attachments.chunked(PHOTOS_PER_ROW).forEach { row ->
            if (row.size == PHOTOS_PER_ROW) {
                Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                    row.forEach { tile(it, Modifier.weight(1f).aspectRatio(1f)) }
                }
            } else {
                val photo = row.single()
                tile(
                    photo,
                    Modifier.fillMaxWidth().aspectRatio(photo.displayRatio().coerceIn(1f, LAST_PHOTO_MAX_RATIO)),
                )
            }
        }
    }
}

private fun Attachment.displayRatio(): Float =
    (aspectRatio ?: DEFAULT_IMAGE_RATIO).coerceIn(MIN_IMAGE_RATIO, MAX_IMAGE_RATIO)

/** The size and shape of the photo are the caller's, so the same photo can fill a bubble or sit inside one. */
@Composable
private fun ImageAttachment(
    attachment: Attachment,
    isFailed: Boolean,
    onClick: (Attachment) -> Unit,
    onShown: (Attachment) -> Unit,
    onRetryDownload: (Attachment) -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(attachment.id, attachment.localPath) { onShown(attachment) }
    val localPath = attachment.localPath
    val description = stringResource(
        if (isFailed) R.string.attachment_photo_failed else R.string.attachment_photo,
        attachment.fileName,
    )
    val clickLabel = stringResource(if (isFailed) R.string.attachment_retry else R.string.attachment_open)
    Box(
        modifier = modifier
            .clickable(enabled = isFailed || localPath != null, onClickLabel = clickLabel) {
                if (isFailed) onRetryDownload(attachment) else onClick(attachment)
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isFailed -> FailedImage()
            localPath == null -> CircularProgressIndicator(modifier = Modifier.size(ProgressSize))
            else -> AsyncImage(
                model = File(localPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FailedImage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(
            text = stringResource(R.string.attachment_failed_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun FileAttachment(attachment: Attachment, isDownloading: Boolean, onClick: (Attachment) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isDownloading) { onClick(attachment) }
            .heightIn(min = MinTouchTarget)
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
