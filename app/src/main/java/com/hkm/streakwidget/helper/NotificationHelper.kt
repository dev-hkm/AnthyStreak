package com.hkm.streakwidget.helper

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.receiver.HabitReminderReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val repository = HabitRepository(context)
    private val localized: Context
        get() = context.localizedFor(repository.getAppLanguage())

    companion object {
        const val CHANNEL_ID = "habit_reminder_channel"
        const val DEBT_CHANNEL_ID = "streak_rescue_channel"

        /** One calm rescue nudge a day; legacy codes are cancelled so old alarms disappear. */
        const val REQUEST_CODE_RESCUE = 8883
        private val LEGACY_RESCUE_CODES = intArrayOf(8881, 8882)
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val resources = localized.resources
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = NotificationChannel(
            CHANNEL_ID,
            resources.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = resources.getString(R.string.notification_channel_reminders_desc)
        }
        manager.createNotificationChannel(reminderChannel)

        val rescueChannel = NotificationChannel(
            DEBT_CHANNEL_ID,
            resources.getString(R.string.notification_channel_rescue_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = resources.getString(R.string.notification_channel_rescue_desc)
            enableVibration(false)
        }
        manager.createNotificationChannel(rescueChannel)
    }

    fun scheduleReminder(habit: Habit) {
        if (!repository.areHabitRemindersEnabled() || !habit.reminderEnabled || habit.reminderTime.isNullOrBlank()) {
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
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
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
     * A single, calm rescue reminder at 19:00. Goals already completed or paused for
     * today never trigger it, and turning the preference off cancels it immediately.
     */
    fun scheduleDebtNagging(habits: List<Habit>) {
        val today = StreakCalculator.todayString()
        val hasActiveDebt = repository.areRescueRemindersEnabled() && habits.any { habit ->
            !habit.isArchived &&
                !habit.isPausedOn(today) &&
                (habit.debts.isNotEmpty() || habit.unclaimedMissedDates(today).isNotEmpty())
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!hasActiveDebt) {
            cancelDebtNagAlarm(alarmManager, REQUEST_CODE_RESCUE)
            LEGACY_RESCUE_CODES.forEach { cancelDebtNagAlarm(alarmManager, it) }
            return
        }

        scheduleDailyDebtNag(alarmManager, 19, 0, REQUEST_CODE_RESCUE)
    }

    private fun scheduleDailyDebtNag(alarmManager: AlarmManager, hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_DEBT_NAG
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
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
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
