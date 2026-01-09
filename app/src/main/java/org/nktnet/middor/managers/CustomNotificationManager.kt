package org.nktnet.middor.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.nktnet.middor.MainActivity
import org.nktnet.middor.R
import org.nktnet.middor.services.MirrorService

object CustomNotificationManager {
    private const val CHANNEL_ID = "mirror"
    private const val CHANNEL_NAME = "Screen Mirroring"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context): Notification {
        val appIntent = Intent(context, MainActivity::class.java)
        val pendingApp = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_MUTABLE
        )
        val exitIntent = Intent(
            context,
            MirrorService::class.java,
        ).apply {
            action = MirrorService.ACTION_STOP_SERVICE
        }
        val pendingExit = PendingIntent.getService(
            context,
            0,
            exitIntent,
            PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.flip_24px)
            .setContentTitle(
                context.getString(R.string.notification_content_title)
            )
            .setContentIntent(pendingApp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_exit_action),
                pendingExit
            )
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
