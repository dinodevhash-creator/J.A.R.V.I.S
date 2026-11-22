package com.example.system

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

private const val BRIEFING_CHANNEL_ID = "jarvis_briefing_channel"
private const val BRIEFING_REQUEST_CODE = 9001
private const val BRIEFING_NOTIFICATION_ID = 9002

class BriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BriefingScheduler.ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = intent?.getStringExtra("extra_title") ?: "J.A.R.V.I.S. // Ringkasan Harian"
        val body = intent?.getStringExtra("extra_body")
            ?: "Selamat beraktivitas, Komandan. Ketuk untuk meninjau direktif aktif dan ringkasan harian Anda."

        val notification = NotificationCompat.Builder(context, BRIEFING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(BRIEFING_NOTIFICATION_ID, notification)
    }
}

object BriefingScheduler {

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(BRIEFING_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    BRIEFING_CHANNEL_ID,
                    "Ringkasan Harian J.A.R.V.I.S.",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi ringkasan pagi dan pengingat direktif terjadwal"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleDailyBriefing(context: Context, hour: Int, minute: Int) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BriefingReceiver::class.java).apply {
            putExtra("extra_title", "J.A.R.V.I.S. // Ringkasan Pagi")
            putExtra("extra_body", "Saatnya meninjau direktif prioritas dan jadwal hari ini bersama J.A.R.V.I.S.")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BriefingReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun sendTestBriefingNotification(context: Context, summaryText: String) {
        val intent = Intent(context, BriefingReceiver::class.java).apply {
            putExtra("extra_title", "J.A.R.V.I.S. // Pengingat Aktif")
            putExtra("extra_body", summaryText.take(200))
        }
        BriefingReceiver().onReceive(context, intent)
    }
}
