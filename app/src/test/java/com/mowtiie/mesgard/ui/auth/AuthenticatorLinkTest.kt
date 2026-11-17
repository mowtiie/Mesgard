package com.mowtiie.mesgard.ui.auth

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthenticatorLinkTest {
    private val link = "otpauth://totp/Mesgard:admin@school.example?secret=ABCD&issuer=Mesgard"

    @Test
    fun `opens the link for any app that handles it`() {
        val intent = authenticatorIntent(link)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(link, intent.dataString)
    }

    @Test
    fun `starts the authenticator in its own task so it never ends up inside Mesgard's screens`() {
        val intent = authenticatorIntent(link)

        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
