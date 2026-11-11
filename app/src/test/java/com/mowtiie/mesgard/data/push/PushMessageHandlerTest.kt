package com.mowtiie.mesgard.data.push

import com.mowtiie.mesgard.data.lifecycle.InMemoryAppVisibility
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PushMessageHandlerTest {
    private val notifier = mockk<MessageNotifier>(relaxed = true)
    private val visibility = InMemoryAppVisibility()
    private val handler = PushMessageHandler(notifier, visibility)
    private val content = """{"conversation_id":"c1","title":"Ana","body":"New message"}""".toByteArray()

    @Test
    fun `notifies while the app is in the background`() {
        handler.handle(content)

        verify(exactly = 1) { notifier.show(IncomingPush("c1", "Ana", "New message")) }
    }

    @Test
    fun `does not notify while the app is in the foreground`() {
        visibility.setForeground(true)

        handler.handle(content)

        verify(exactly = 0) { notifier.show(any()) }
    }

    @Test
    fun `does not notify for a message that cannot be read`() {
        handler.handle("not json".toByteArray())
        handler.handle("""{"title":"Ana"}""".toByteArray())

        verify(exactly = 0) { notifier.show(any()) }
    }
}
