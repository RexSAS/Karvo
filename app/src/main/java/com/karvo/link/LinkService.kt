package com.karvo.link

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karvo.KarvoApp
import com.karvo.MainActivity
import com.karvo.R

/** Keeps the link alive while a PC is connected; its notification offers Disconnect (spec "Phone behaviour"). */
class LinkService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Started and stopped within milliseconds: startForeground must still be called once, or Android crashes the app.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(ID, notification(this, emptyList()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(ID, notification(this, emptyList()))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_DISCONNECT) {
            (application as KarvoApp).container.linkManager.disconnectAll()
            return START_NOT_STICKY
        }
        val n = notification(this, intent?.getStringArrayListExtra(EXTRA_NAMES).orEmpty())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(ID, n)
        return START_NOT_STICKY
    }

    /** Android 15 caps dataSync services at 6 hours a day: disconnect cleanly instead of being killed. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        (application as KarvoApp).container.linkManager.disconnectAll()
        stopSelf()
    }

    companion object {
        private const val ID = 4207
        private const val CHANNEL = "link"
        private const val EXTRA_NAMES = "names"
        private const val ACTION_DISCONNECT = "com.karvo.link.DISCONNECT"
        private const val ACTION_STOP = "com.karvo.link.STOP"

        fun notification(context: Context, names: List<String>): Notification {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.link_notification_channel), NotificationManager.IMPORTANCE_LOW))
            }
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val disconnect = PendingIntent.getService(context, 1,
                Intent(context, LinkService::class.java).setAction(ACTION_DISCONNECT), PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_link_notification)
                .setContentTitle(context.getString(R.string.link_notification_title, names.joinToString(", ")))
                .setContentText(context.getString(R.string.link_notification_text))
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(0, context.getString(R.string.link_disconnect), disconnect)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }

        fun starter(context: Context): ServiceStarter = object : ServiceStarter {
            private val app = context.applicationContext
            override fun start(names: List<String>) = ContextCompat.startForegroundService(app,
                Intent(app, LinkService::class.java).putStringArrayListExtra(EXTRA_NAMES, ArrayList(names)))
            override fun update(names: List<String>) {
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                    NotificationManagerCompat.from(app).notify(ID, notification(app, names))
            }
            override fun stop() {
                runCatching { app.startService(Intent(app, LinkService::class.java).setAction(ACTION_STOP)) }
                    .onFailure { app.stopService(Intent(app, LinkService::class.java)) }
            }
        }
    }
}
