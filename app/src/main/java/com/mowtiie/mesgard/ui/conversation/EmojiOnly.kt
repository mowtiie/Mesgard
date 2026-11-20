package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message

/** A message of this many emoji or fewer, and nothing else, is shown large and without a bubble. */
internal const val MAX_LARGE_EMOJI = 3

private const val ZERO_WIDTH_JOINER = 0x200D
private const val VARIATION_SELECTOR = 0xFE0F
private const val KEYCAP = 0x20E3
private val SkinTones = 0x1F3FB..0x1F3FF
private val TagCharacters = 0xE0020..0xE007F
private val RegionalIndicators = 0x1F1E6..0x1F1FF
private val EmojiBlocks = listOf(0x1F000..0x1FAFF, 0x2600..0x27BF, 0x2300..0x23FF, 0x2B00..0x2BFF)

/**
 * Whether the text is one to [MAX_LARGE_EMOJI] emoji with at most spaces between them. A joined sequence (a family,
 * a skin tone, a flag) counts as one. This is a judgment of the text only, so it needs no emoji library.
 */
internal fun isEmojiOnly(text: String): Boolean = countEmoji(text) in 1..MAX_LARGE_EMOJI

/** The number of emoji in the text, or null when it holds anything that is not an emoji or a space. */
private fun countEmoji(text: String): Int? {
    val counter = EmojiCounter()
    return if (text.codePoints().toArray().all(counter::accept)) counter.clusters else null
}

/** Counts the emoji of a text one code point at a time; a joined sequence or a flag is one. */
private class EmojiCounter {
    var clusters = 0
        private set
    private var joinsNext = false
    private var openFlag = false

    /** Returns false for a code point that is not part of an emoji. */
    fun accept(codePoint: Int): Boolean {
        when {
            Character.isWhitespace(codePoint) -> joinsNext = false
            codePoint == ZERO_WIDTH_JOINER -> joinsNext = true
            isSequenceModifier(codePoint) -> Unit
            codePoint in RegionalIndicators -> startFlagOrFinishIt()
            EmojiBlocks.any { codePoint in it } -> startEmoji()
            else -> return false
        }
        return true
    }

    private fun startFlagOrFinishIt() {
        if (!openFlag) clusters++
        openFlag = !openFlag
    }

    private fun startEmoji() {
        if (!joinsNext) clusters++
        joinsNext = false
        openFlag = false
    }
}

/** A code point that changes the emoji before it, so it never starts another one. */
private fun isSequenceModifier(codePoint: Int) =
    codePoint == VARIATION_SELECTOR || codePoint == KEYCAP || codePoint in SkinTones || codePoint in TagCharacters

internal fun Message.isEmojiOnlyMessage(): Boolean =
    !isDeleted && attachments.isEmpty() && replyTo == null && isEmojiOnly(body)
