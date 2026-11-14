package com.mowtiie.mesgard.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordPolicyTest {
    @Test
    fun `accepts a long enough password that matches its confirmation`() {
        assertNull(findPasswordProblem("a-good-one", "a-good-one"))
    }

    @Test
    fun `refuses a password that is too short before comparing it`() {
        assertEquals(PasswordProblem.TOO_SHORT, findPasswordProblem("short", "different"))
        assertEquals(PasswordProblem.TOO_SHORT, findPasswordProblem("1234567", "1234567"))
    }

    @Test
    fun `accepts exactly the minimum length`() {
        assertNull(findPasswordProblem("12345678", "12345678"))
    }

    @Test
    fun `refuses a confirmation that differs`() {
        assertEquals(PasswordProblem.MISMATCH, findPasswordProblem("long-enough-1", "long-enough-2"))
    }
}
