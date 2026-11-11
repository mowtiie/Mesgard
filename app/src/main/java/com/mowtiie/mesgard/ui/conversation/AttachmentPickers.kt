package com.mowtiie.mesgard.ui.conversation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mowtiie.mesgard.domain.attachment.ALLOWED_ATTACHMENT_TYPES
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENTS_PER_MESSAGE

internal class AttachmentPickers(val pickPhotos: () -> Unit, val pickFiles: () -> Unit)

/** Opens the system photo picker and document picker, which need no storage permission. */
@Composable
internal fun rememberAttachmentPickers(onPicked: (List<PickedAttachment>) -> Unit): AttachmentPickers {
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS_PER_MESSAGE),
    ) { uris -> onPicked(uris.map { it.toPicked(context) }) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        onPicked(uris.map { it.toPicked(context) })
    }
    return remember(photoLauncher, fileLauncher) {
        AttachmentPickers(
            pickPhotos = {
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            pickFiles = { fileLauncher.launch(ALLOWED_ATTACHMENT_TYPES.toTypedArray()) },
        )
    }
}

private fun Uri.toPicked(context: Context): PickedAttachment {
    val name = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) it.getString(index) else null
    }
    return PickedAttachment(uri = toString(), name = name ?: lastPathSegment.orEmpty())
}
