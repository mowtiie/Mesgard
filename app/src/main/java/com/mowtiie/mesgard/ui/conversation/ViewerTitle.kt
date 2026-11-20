package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
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
