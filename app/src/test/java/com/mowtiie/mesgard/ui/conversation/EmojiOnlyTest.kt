package com.mowtiie.mesgard.ui.conversation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiOnlyTest {
    @Test
    fun `one to three emoji are large`() {
        assertTrue(isEmojiOnly("😀"))
        assertTrue(isEmojiOnly("😀😀 👍"))
    }

    @Test
    fun `four emoji are not large`() {
        assertFalse(isEmojiOnly("😀😀😀😀"))
    }

    @Test
    fun `text with an emoji is not large`() {
        assertFalse(isEmojiOnly("ok 👍"))
        assertFalse(isEmojiOnly("1"))
    }

    @Test
    fun `empty or blank text is not large`() {
        assertFalse(isEmojiOnly(""))
        assertFalse(isEmojiOnly("  "))
    }

    @Test
    fun `a skin tone and a joined family each count as one`() {
        val thumbsUpWithTone = "👍🏽"
        val family = "👨‍👩‍👧‍👦"
        assertTrue(isEmojiOnly(thumbsUpWithTone + family + thumbsUpWithTone))
    }

    @Test
    fun `a flag of two regional indicators counts as one`() {
        val flag = "🇵🇭"
        assertTrue(isEmojiOnly(flag + flag + flag))
        assertFalse(isEmojiOnly(flag + flag + flag + flag))
    }

    @Test
    fun `a heart with a variation selector is one emoji`() {
        assertTrue(isEmojiOnly("❤️"))
    }
}
