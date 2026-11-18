package com.mowtiie.mesgard.data.push

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MessageNotifierTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifier = AndroidMessageNotifier(application)
    private val push = IncomingPush("c1", "Ana Reyes", "New message")

    private fun grantPermission() = shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    private fun conversationNotifications() = shadowOf(manager).allNotifications.filter {
        it.flags and
            Notification.FLAG_GROUP_SUMMARY ==
            0
    }

    private fun summary() = shadowOf(manager).allNotifications.single {
        it.flags and Notification.FLAG_GROUP_SUMMARY !=
            0
    }

    private fun lines(notification: Notification) =
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)!!.messages.map {
            it.text.toString()
        }

    @Test
    fun `shows the title and body prepared by the server`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = conversationNotifications().single()
        assertEquals("Ana Reyes", notification.extras.getString("android.title"))
        assertEquals("New message", notification.extras.getString("android.text"))
    }

    @Test
    fun `keeps the content off the lock screen with a generic public version`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = conversationNotifications().single()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals("Mesgard", notification.publicVersion.extras.getString("android.title"))
        assertEquals("New message", notification.publicVersion.extras.getString("android.text"))
    }

    @Test
    fun `opens the conversation when tapped`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = conversationNotifications().single()
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals("c1", intent.getStringExtra(EXTRA_CONVERSATION_ID))
        assertTrue(notification.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun `stacks the messages of one conversation in a single notification`() {
        grantPermission()

        notifier.show(push.copy(body = "one"))
        notifier.show(push.copy(body = "two"))
        notifier.show(push.copy(body = "three"))

        assertEquals(listOf("one", "two", "three"), lines(conversationNotifications().single()))
    }

    @Test
    fun `keeps only the latest few messages of a conversation`() {
        grantPermission()

        (1..MAX_STACKED_MESSAGES + 3).forEach { notifier.show(push.copy(body = "m$it")) }

        val shown = lines(conversationNotifications().single())
        assertEquals(MAX_STACKED_MESSAGES, shown.size)
        assertEquals("m${MAX_STACKED_MESSAGES + 3}", shown.last())
    }

    @Test
    fun `groups the conversations under one summary that counts them`() {
        grantPermission()

        notifier.show(push)
        notifier.show(push.copy(conversationId = "c2", title = "Ben"))

        assertEquals(2, conversationNotifications().size)
        assertEquals("2 conversations with new messages", summary().extras.getString("android.text"))
    }

    @Test
    fun `the summary never carries message text`() {
        grantPermission()

        notifier.show(push.copy(body = "a secret plan"))

        assertTrue(!summary().extras.getString("android.text").orEmpty().contains("secret"))
        assertEquals("Mesgard", summary().publicVersion.extras.getString("android.title"))
    }

    @Test
    fun `clearing a conversation removes its notification and then the summary`() {
        grantPermission()
        notifier.show(push)
        notifier.show(push.copy(conversationId = "c2", title = "Ben"))

        notifier.clear("c1")
        assertEquals(1, conversationNotifications().size)
        assertEquals("1 conversation with new messages", summary().extras.getString("android.text"))

        notifier.clear("c2")
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `creates the messages channel with high importance`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val channel = manager.getNotificationChannel(MESSAGES_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun `shows nothing without the notification permission`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}
