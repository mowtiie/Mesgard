package com.mowtiie.mesgard.data.push

import android.Manifest
import android.app.Application
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

    @Test
    fun `shows the title and body prepared by the server`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = shadowOf(manager).allNotifications.single()
        assertEquals("Ana Reyes", notification.extras.getString("android.title"))
        assertEquals("New message", notification.extras.getString("android.text"))
    }

    @Test
    fun `keeps the content off the lock screen with a generic public version`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = shadowOf(manager).allNotifications.single()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals("Mesgard", notification.publicVersion.extras.getString("android.title"))
        assertEquals("New message", notification.publicVersion.extras.getString("android.text"))
    }

    @Test
    fun `opens the conversation when tapped`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)

        val notification = shadowOf(manager).allNotifications.single()
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals("c1", intent.getStringExtra(EXTRA_CONVERSATION_ID))
        assertTrue(notification.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun `replaces the notification of the same conversation`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(push)
        notifier.show(push.copy(title = "Ben"))

        assertEquals(1, shadowOf(manager).allNotifications.size)
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
