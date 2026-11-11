package com.mowtiie.mesgard.data.push

import com.mowtiie.mesgard.data.lifecycle.InMemoryAppVisibility
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PushMessageHandlerTest {
    private val notifier = mockk<MessageNotifier>(relaxed = true)
    private val visibility = InMemoryAppVisibility()
    private val handler = PushMessageHandler(notifier, visibility)
    private val data = mapOf("conversation_id" to "c1", "title" to "Ana", "body" to "New message")

    @Test
    fun `notifies while the app is in the background`() {
        handler.handle(data)

        verify(exactly = 1) { notifier.show(IncomingPush("c1", "Ana", "New message")) }
    }

    @Test
    fun `does not notify while the app is in the foreground`() {
        visibility.setForeground(true)

        handler.handle(data)

        verify(exactly = 0) { notifier.show(any()) }
    }

    @Test
    fun `does not notify for a message that cannot be read`() {
        handler.handle(mapOf("title" to "Ana"))

        verify(exactly = 0) { notifier.show(any()) }
    }
}
