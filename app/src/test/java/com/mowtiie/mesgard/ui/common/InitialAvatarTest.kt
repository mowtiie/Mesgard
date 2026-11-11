package com.mowtiie.mesgard.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialAvatarTest {
    @Test
    fun `the same name always gets the same color`() {
        assertEquals(avatarColorIndex("Ana Reyes"), avatarColorIndex("Ana Reyes"))
    }

    @Test
    fun `case and surrounding spaces do not change the color`() {
        assertEquals(avatarColorIndex("Ana Reyes"), avatarColorIndex("  ana reyes "))
    }

    @Test
    fun `the color index is always within the palette`() {
        val names = listOf("", "A", "Ana", "Student One RNHS", "éè", "x".repeat(500), "Zoe", "-1")
        names.forEach { assertTrue(avatarColorIndex(it) in 0..2) }
    }

    @Test
    fun `different names use more than one color`() {
        val indexes = listOf("Ana", "Ben", "Carla", "Dev", "Elena", "Faye", "Gil", "Hana").map(::avatarColorIndex)

        assertTrue(indexes.toSet().size > 1)
    }
}
