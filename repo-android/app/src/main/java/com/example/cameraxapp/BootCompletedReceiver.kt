package com.example.cameraxapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = AgendaDatabaseHelper(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val now = System.currentTimeMillis()

            // 1. Reschedule Active Alarms that are in the future
            val alarms = dbHelper.getAllAlarms()
            for (alarm in alarms) {
                if (alarm.isActive && alarm.timeMillis > now) {
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("ALARM_ID", alarm.id)
                        putExtra("ALARM_LABEL", alarm.label)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (BuildHelper.canScheduleExact(context)) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarm.timeMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            alarm.timeMillis,
                            pendingIntent
                        )
                    }
                } else if (alarm.isActive && alarm.timeMillis <= now) {
                    // Turn off past once-off alarms
                    dbHelper.updateAlarmStatus(alarm.id, false)
                }
            }

            // 2. Reschedule future calendar events
            val events = dbHelper.getAllEvents()
            for (event in events) {
                if (event.dateMillis > now) {
                    val eventIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("ALARM_ID", event.id + 100000) // offset calendar events to prevent overlap with alarm IDs
                        putExtra("ALARM_LABEL", "Upcoming: ${event.title}")
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        event.id + 100000,
                        eventIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        event.dateMillis,
                        pendingIntent
                    )
                }
            }
        }
    }
}

object BuildHelper {
    fun canScheduleExact(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
