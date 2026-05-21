package com.example.cameraxapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "AGENDA_ALARM_CHANNEL"
        const val CHANNEL_NAME = "Agenda Alarms & Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Agenda Reminder"

        // 1. Create and post notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires high priority notifications for alarms and tasks"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🍓 Fraise Agenda Alert")
            .setContentText(alarmLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(ringtoneUri)
            .setAutoCancel(true)

        notificationManager.notify(alarmId, builder.build())

        // 2. Perform vibration
        vibratePhone(context)

        // 3. Mark alarm as inactive in local SQLite DB if it was a one-off
        if (alarmId != -1) {
            val dbHelper = AgendaDatabaseHelper(context)
            dbHelper.updateAlarmStatus(alarmId, false)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibratePhone(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200, 500)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    it.vibrate(1500)
                }
            }
        }
    }
}
