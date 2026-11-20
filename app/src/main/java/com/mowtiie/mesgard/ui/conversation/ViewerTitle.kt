package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.Message

/** Who sent the photo, or the file name when the message is not known. */
@Composable
internal fun viewerTitle(message: Message?, fileName: String): String = when {
    message == null -> fileName
    message.isOwn -> stringResource(R.string.notification_you)
    else -> message.senderName ?: fileName
}

/** The day and time the photo was sent, in the format of the device. */
@Composable
internal fun viewerSubtitle(epochMillis: Long): String {
    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
    return DateUtils.formatDateTime(LocalContext.current, epochMillis, flags)
}

/** The photos of one message that are on the device, and which of them the person opened. */
internal data class ViewerPhotos(val photos: List<Attachment>, val startIndex: Int)

internal fun viewerPhotosOf(message: Message, openedId: String): ViewerPhotos? {
    val photos = message.attachments.filter { it.isImage && it.localPath != null }
    val index = photos.indexOfFirst { it.id == openedId }
    return if (index >= 0) ViewerPhotos(photos, index) else null
}
