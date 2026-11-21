package com.mowtiie.mesgard.ui.about

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FINGERPRINT_BYTES = 32

class AboutTest {
    @Test
    fun `formats a digest as upper case pairs separated by colons`() {
        assertEquals("00:0F:A0:FF", formatFingerprint(byteArrayOf(0x00, 0x0F, 0xA0.toByte(), 0xFF.toByte())))
    }

    @Test
    fun `formats a SHA-256 digest like keytool does`() {
        val digest = MessageDigest.getInstance("SHA-256").digest(ByteArray(0))

        assertEquals(
            "E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55",
            formatFingerprint(digest),
        )
    }

    @Test
    fun `breaks a fingerprint into lines of eight bytes without splitting a pair`() {
        val fingerprint = formatFingerprint(ByteArray(FINGERPRINT_BYTES) { it.toByte() })

        val lines = fingerprintLines(fingerprint).lines()

        assertEquals(4, lines.size)
        assertTrue(lines.all { it.split(":").size == 8 })
        assertEquals(fingerprint, lines.joinToString(":"))
    }

    @Test
    fun `lists every third party notice once and names its license`() {
        val names = ThirdPartyNotices.map { it.name }

        assertEquals(names.size, names.toSet().size)
        assertTrue(ThirdPartyNotices.all { it.license.isNotBlank() })
    }
}
