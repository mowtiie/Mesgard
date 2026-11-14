package com.mowtiie.mesgard.data.auth

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordChangeMarkTest {
    @Test
    fun `an account marked by an admin must change its password`() {
        assertTrue(buildJsonObject { put("must_change_password", true) }.requiresPasswordChange())
    }

    @Test
    fun `an account without the mark does not`() {
        assertFalse(null.requiresPasswordChange())
        assertFalse(JsonObject(emptyMap()).requiresPasswordChange())
        assertFalse(buildJsonObject { put("provider", "email") }.requiresPasswordChange())
    }

    @Test
    fun `only a true value counts as the mark`() {
        assertFalse(buildJsonObject { put("must_change_password", false) }.requiresPasswordChange())
        assertFalse(JsonObject(mapOf("must_change_password" to JsonPrimitive("yes"))).requiresPasswordChange())
    }
}
