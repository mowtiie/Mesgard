package com.mowtiie.mesgard.ui.admin

import android.content.ClipData
import android.os.PersistableBundle
import androidx.compose.ui.platform.ClipEntry

private const val SENSITIVE_CONTENT_EXTRA = "android.content.extra.IS_SENSITIVE"

/** A clipboard entry marked as sensitive, so Android hides it from clipboard previews. */
internal fun sensitiveClip(text: String): ClipEntry {
    val clip = ClipData.newPlainText(null, text)
    clip.description.extras = PersistableBundle().apply { putBoolean(SENSITIVE_CONTENT_EXTRA, true) }
    return ClipEntry(clip)
}
