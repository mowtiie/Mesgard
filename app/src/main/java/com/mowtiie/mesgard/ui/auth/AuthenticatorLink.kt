package com.mowtiie.mesgard.ui.auth

import android.content.Intent
import androidx.core.net.toUri

/**
 * The otpauth link that an authenticator app opens. It starts in its own task: without that the app is put on top of
 * Mesgard's own screens, and opening Mesgard again shows the authenticator instead of the setup screen.
 */
internal fun authenticatorIntent(link: String): Intent =
    Intent(Intent.ACTION_VIEW, link.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
