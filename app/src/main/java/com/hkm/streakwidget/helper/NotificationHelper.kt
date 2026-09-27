package com.hkm.streakwidget.helper

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.receiver.HabitReminderReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "habit_reminder_channel"
        const val CHANNEL_NAME = "Habit Reminders"
        const val CHANNEL_DESC = "Notifications reminding you of your daily habits"

        const val DEBT_CHANNEL_ID = "streak_debt_channel"
        const val DEBT_CHANNEL_NAME = "Streak Debt Urgency"
        const val DEBT_CHANNEL_DESC = "Urgent daily alerts and reminders to pay streak debts"

        const val REQUEST_CODE_DEBT_MORNING = 8881
        const val REQUEST_CODE_DEBT_EVENING = 8882
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val standardChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(standardChannel)

            val debtChannel = NotificationChannel(DEBT_CHANNEL_ID, DEBT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = DEBT_CHANNEL_DESC
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(debtChannel)
        }
    }

    fun scheduleReminder(habit: Habit) {
        if (!habit.reminderEnabled || habit.reminderTime.isNullOrBlank()) {
            cancelReminder(habit)
            return
        }

        val parts = habit.reminderTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra("HABIT_ID", habit.id)
            putExtra("HABIT_NAME", habit.name)
            putExtra("HABIT_NOTE", habit.motivationNote)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun cancelReminder(habit: Habit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Relentlessly schedules daily nagging & warning notifications at 10:00 and 20:30
     * whenever any active habit has debts or unclaimed missed days.
     */
    fun scheduleDebtNagging(habits: List<Habit>) {
        val hasActiveDebt = habits.any { !it.isArchived && (it.debts.isNotEmpty() || it.unclaimedMissedDates().isNotEmpty()) }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!hasActiveDebt) {
            cancelDebtNagAlarm(alarmManager, REQUEST_CODE_DEBT_MORNING)
            cancelDebtNagAlarm(alarmManager, REQUEST_CODE_DEBT_EVENING)
            return
        }

        scheduleDailyDebtNag(alarmManager, 10, 0, REQUEST_CODE_DEBT_MORNING)
        scheduleDailyDebtNag(alarmManager, 20, 30, REQUEST_CODE_DEBT_EVENING)
    }

    private fun scheduleDailyDebtNag(alarmManager: AlarmManager, hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_DEBT_NAG
            putExtra("NAG_HOUR", hour)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun cancelDebtNagAlarm(alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_DEBT_NAG
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
