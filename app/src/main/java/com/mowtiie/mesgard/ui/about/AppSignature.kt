package com.mowtiie.mesgard.ui.about

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

/** Formats a digest the way `apksigner` and `keytool` print certificate fingerprints: upper case pairs with colons. */
internal fun formatFingerprint(digest: ByteArray): String = digest.joinToString(":") { "%02X".format(it) }

private const val BYTES_PER_LINE = 8

/** Breaks a fingerprint after every eighth byte, so a line never ends in the middle of a pair. */
internal fun fingerprintLines(fingerprint: String): String =
    fingerprint.split(":").chunked(BYTES_PER_LINE).joinToString("\n") { it.joinToString(":") }

/**
 * The SHA-256 fingerprint of the certificate this build is signed with, so people can compare it with the one published
 * for releases. It is null when the platform reports no signature.
 */
internal fun signingFingerprint(context: Context): String? {
    val signature = PackageInfoCompat.getSignatures(context.packageManager, context.packageName).firstOrNull()
        ?: return null
    return formatFingerprint(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()))
}
