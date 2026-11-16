package com.mowtiie.mesgard.ui.about

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

/** Formats a digest the way `apksigner` and `keytool` print certificate fingerprints: upper case pairs with colons. */
internal fun formatFingerprint(digest: ByteArray): String = digest.joinToString(":") { "%02X".format(it) }

/**
 * The SHA-256 fingerprint of the certificate this build is signed with, so people can compare it with the one published
 * for releases. It is null when the platform reports no signature.
 */
internal fun signingFingerprint(context: Context): String? {
    val signature = PackageInfoCompat.getSignatures(context.packageManager, context.packageName).firstOrNull()
        ?: return null
    return formatFingerprint(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()))
}
