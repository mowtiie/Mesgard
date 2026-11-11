package com.mowtiie.mesgard.data.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingPushTest {
    @Test
    fun `reads the conversation, title and body`() {
        val push = IncomingPush.fromData(mapOf("conversation_id" to "c1", "title" to "Ana", "body" to "New message"))

        assertEquals(IncomingPush("c1", "Ana", "New message"), push)
    }

    @Test
    fun `ignores a message without a conversation`() {
        assertNull(IncomingPush.fromData(mapOf("title" to "Ana", "body" to "New message")))
    }

    @Test
    fun `ignores a message with a blank title or body`() {
        assertNull(IncomingPush.fromData(mapOf("conversation_id" to "c1", "title" to " ", "body" to "New message")))
        assertNull(IncomingPush.fromData(mapOf("conversation_id" to "c1", "title" to "Ana", "body" to "")))
    }
}
