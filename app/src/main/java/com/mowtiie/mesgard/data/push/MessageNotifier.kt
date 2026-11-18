package com.mowtiie.mesgard.data.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.push.ConversationNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal const val MESSAGES_CHANNEL_ID = "messages"
internal const val MAX_STACKED_MESSAGES = 5

internal interface MessageNotifier {
    fun show(push: IncomingPush)
}

/**
 * One notification per conversation that stacks its latest messages, and one summary that groups the conversations.
 * The earlier lines are read back from the notification that is already showing, so no message text is stored.
 */
internal class AndroidMessageNotifier @Inject constructor(@param:ApplicationContext private val context: Context) :
    MessageNotifier,
    ConversationNotifications {
    private val notifications = NotificationManagerCompat.from(context)
    private val factory = MessageNotificationFactory(context)

    // canNotify() checks the permission, which lint cannot see through the helper.
    @SuppressLint("MissingPermission")
    override fun show(push: IncomingPush) {
        if (!canNotify()) return
        createChannel()
        val id = notificationId(push.conversationId)
        notifications.notify(id, factory.conversation(push, earlierMessages(id)))
        notifications.notify(SUMMARY_NOTIFICATION_ID, factory.summary(conversationIds() + id))
    }

    @SuppressLint("MissingPermission")
    override fun clear(conversationId: String) {
        val id = notificationId(conversationId)
        val remaining = conversationIds() - id
        notifications.cancel(id)
        if (remaining.isEmpty()) {
            notifications.cancel(SUMMARY_NOTIFICATION_ID)
        } else if (canNotify()) {
            notifications.notify(SUMMARY_NOTIFICATION_ID, factory.summary(remaining))
        }
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun active(): List<StatusBarNotification> = notifications.activeNotifications

    private fun conversationIds(): Set<Int> = active().map { it.id }.filter { it != SUMMARY_NOTIFICATION_ID }.toSet()

    private fun earlierMessages(id: Int): List<NotificationCompat.MessagingStyle.Message> = active()
        .firstOrNull { it.id == id }
        ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it.notification) }
        ?.messages
        .orEmpty()

    private fun createChannel() {
        val channel = NotificationChannel(
            MESSAGES_CHANNEL_ID,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
