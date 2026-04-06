package com.example.sdn4angsau.samosa

import android.Manifest
import android.app.AlarmManager
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
import androidx.core.content.edit
import kotlin.math.abs

object TempatSampahNotificationHelper {

    private const val CHANNEL_ID = "samosa_bin_alerts"
    private const val PREF_NAME = "SamosaNotificationState"
    private const val KEY_PREFIX_LAST_ALERT = "last_alert_"
    private const val REMINDER_INTERVAL_MS = 60 * 60 * 1000L

    const val EXTRA_BIN_ID = "extra_bin_id"
    const val EXTRA_LOKASI = "extra_lokasi"
    const val EXTRA_PERSENTASE = "extra_persentase"

    fun syncNotifications(context: Context, bins: List<TempatSampah>) {
        ensureNotificationChannel(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        bins.filterNot { it.isFull }.forEach { bin ->
            cancelReminder(context, bin.binId)
            NotificationManagerCompat.from(context).cancel(notificationId(bin.binId))
            prefs.edit {
                remove("$KEY_PREFIX_LAST_ALERT${bin.binId}")
            }
        }

        if (!canPostNotifications(context)) {
            return
        }

        val now = System.currentTimeMillis()

        bins.filter { it.isFull }.forEach { bin ->
            val key = "$KEY_PREFIX_LAST_ALERT${bin.binId}"
            val lastAlert = prefs.getLong(key, 0L)

            if (now - lastAlert >= REMINDER_INTERVAL_MS) {
                showImmediateNotification(context, bin)
                prefs.edit {
                    putLong(key, now)
                }
            }

            scheduleReminder(context, bin)
        }
    }

    fun showReminderNotification(
        context: Context,
        lokasi: String,
        binId: String,
        persentase: Int
    ) {
        if (!canPostNotifications(context)) {
            return
        }

        ensureNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(
                context.getString(
                    R.string.notification_reminder_message,
                    lokasi,
                    binId,
                    persentase
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.notification_reminder_message,
                        lokasi,
                        binId,
                        persentase
                    )
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createDashboardPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(binId), notification)
    }

    private fun showImmediateNotification(context: Context, bin: TempatSampah) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentTitle(context.getString(R.string.notification_full_title))
            .setContentText(
                context.getString(
                    R.string.notification_full_message,
                    bin.lokasi,
                    bin.binId,
                    bin.persentase
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.notification_full_message,
                        bin.lokasi,
                        bin.binId,
                        bin.persentase
                    )
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createDashboardPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(bin.binId), notification)
    }

    private fun createDashboardPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleReminder(context: Context, bin: TempatSampah) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createReminderPendingIntent(context, bin)

        alarmManager.cancel(pendingIntent)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + REMINDER_INTERVAL_MS,
            REMINDER_INTERVAL_MS,
            pendingIntent
        )
    }

    private fun cancelReminder(context: Context, binId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createReminderPendingIntent(
            context = context,
            bin = TempatSampah(binId = binId, lokasi = "", persentase = 0)
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createReminderPendingIntent(
        context: Context,
        bin: TempatSampah
    ): PendingIntent {
        val intent = Intent(context, TempatSampahReminderReceiver::class.java).apply {
            putExtra(EXTRA_BIN_ID, bin.binId)
            putExtra(EXTRA_LOKASI, bin.lokasi)
            putExtra(EXTRA_PERSENTASE, bin.persentase)
        }

        return PendingIntent.getBroadcast(
            context,
            reminderRequestCode(bin.binId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderRequestCode(binId: String): Int = 4000 + abs(binId.hashCode() % 1000)

    private fun notificationId(binId: String): Int = 2000 + abs(binId.hashCode() % 1000)

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        manager.createNotificationChannel(channel)
    }
}
