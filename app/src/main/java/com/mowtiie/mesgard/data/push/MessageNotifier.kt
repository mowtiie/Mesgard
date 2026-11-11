package com.mowtiie.mesgard.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mowtiie.mesgard.MainActivity
import com.mowtiie.mesgard.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal const val MESSAGES_CHANNEL_ID = "messages"

internal interface MessageNotifier {
    fun show(push: IncomingPush)
}

internal class AndroidMessageNotifier @Inject constructor(@param:ApplicationContext private val context: Context) :
    MessageNotifier {
    override fun show(push: IncomingPush) {
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canNotify) return

        createChannel()
        val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion())
            .setContentIntent(openConversation(push.conversationId))
            .build()
        NotificationManagerCompat.from(context).notify(push.conversationId.hashCode(), notification)
    }

    private fun publicVersion() = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_chat)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.notification_new_message))
        .build()

    private fun openConversation(conversationId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_CONVERSATION_ID, conversationId)
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            MESSAGES_CHANNEL_ID,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
