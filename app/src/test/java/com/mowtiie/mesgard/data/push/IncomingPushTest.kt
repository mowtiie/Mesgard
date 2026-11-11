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

    @Test
    fun `reads the JSON content of a decrypted push message`() {
        val content = """{"conversation_id":"c1","title":"Ana","body":"New message"}""".toByteArray()

        assertEquals(IncomingPush("c1", "Ana", "New message"), IncomingPush.fromContent(content))
    }

    @Test
    fun `ignores content that is not a JSON object of strings`() {
        assertNull(IncomingPush.fromContent("hello".toByteArray()))
        assertNull(IncomingPush.fromContent("[1,2]".toByteArray()))
        assertNull(IncomingPush.fromContent("""{"title":1}""".toByteArray()))
    }
}
