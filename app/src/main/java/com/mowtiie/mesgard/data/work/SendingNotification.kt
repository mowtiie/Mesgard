package com.mowtiie.mesgard.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.mowtiie.mesgard.R

private const val SENDING_CHANNEL_ID = "sending"
private const val SENDING_NOTIFICATION_ID = 2

/**
 * Expedited work runs as a foreground service before Android 12, which needs a notification. It is shown at the
 * lowest importance and only while a message is being sent.
 */
internal fun Context.sendingForegroundInfo(): ForegroundInfo {
    val channel = NotificationChannel(
        SENDING_CHANNEL_ID,
        getString(R.string.notification_channel_sending),
        NotificationManager.IMPORTANCE_MIN,
    )
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    val notification = NotificationCompat.Builder(this, SENDING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_chat)
        .setContentTitle(getString(R.string.notification_sending_message))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build()
    return ForegroundInfo(SENDING_NOTIFICATION_ID, notification)
}
