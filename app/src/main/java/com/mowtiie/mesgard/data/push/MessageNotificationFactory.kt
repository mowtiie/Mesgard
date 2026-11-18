package com.mowtiie.mesgard.data.push

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.mowtiie.mesgard.MainActivity
import com.mowtiie.mesgard.R

internal const val SUMMARY_NOTIFICATION_ID = 0
private const val MESSAGES_GROUP_KEY = "com.mowtiie.mesgard.MESSAGES"

/** The notification of a conversation keeps its latest lines; the summary only counts the conversations. */
internal class MessageNotificationFactory(private val context: Context) {
    fun conversation(push: IncomingPush, earlier: List<NotificationCompat.MessagingStyle.Message>): Notification {
        val style = NotificationCompat.MessagingStyle(person(context.getString(R.string.notification_you)))
        earlier.takeLast(MAX_STACKED_MESSAGES - 1).forEach(style::addMessage)
        style.addMessage(push.body, System.currentTimeMillis(), person(push.title))
        return NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setStyle(style)
            .setGroup(MESSAGES_GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion())
            .setContentIntent(openConversation(push.conversationId))
            .build()
    }

    fun summary(conversations: Set<Int>): Notification {
        val count = conversations.size
        return NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.resources.getQuantityString(R.plurals.notification_conversations, count, count))
            .setGroup(MESSAGES_GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion())
            .setContentIntent(openApp())
            .build()
    }

    private fun person(name: String) = Person.Builder().setName(name).build()

    private fun publicVersion() = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_chat)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.notification_new_message))
        .build()

    private fun openConversation(conversationId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_CONVERSATION_ID, conversationId)
        return pendingActivity(notificationId(conversationId), intent)
    }

    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return pendingActivity(SUMMARY_NOTIFICATION_ID, intent)
    }

    private fun pendingActivity(requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** The summary owns id 0, so a conversation whose id hashes to 0 takes the next one. */
internal fun notificationId(conversationId: String): Int =
    conversationId.hashCode().takeIf { it != SUMMARY_NOTIFICATION_ID } ?: 1
