package uk.nktnet.middor.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import uk.nktnet.middor.MirrorService

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
        val exitIntent = Intent(context, MirrorService::class.java).apply {
            action = "ACTION_STOP_SERVICE"
        }
        val pendingExit = PendingIntent.getService(
            context,
            0,
            exitIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Screen mirroring active")
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Exit",
                pendingExit
            )
            .build()
    }
}
