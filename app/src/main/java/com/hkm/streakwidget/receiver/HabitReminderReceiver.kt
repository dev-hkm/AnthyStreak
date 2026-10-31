package com.hkm.streakwidget.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.helper.NotificationHelper
import com.hkm.streakwidget.model.Habit

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HABIT_REMINDER = "com.hkm.streakwidget.receiver.HABIT_REMINDER"
        const val ACTION_DEBT_NAG = "com.hkm.streakwidget.receiver.DEBT_NAG"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repository = HabitRepository(context)
        val notifications = NotificationHelper(context)
        val allHabits = repository.getAllHabits()
        val localized = context.localizedFor(repository.getAppLanguage())

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (repository.areHabitRemindersEnabled()) {
                    allHabits.filter { it.reminderEnabled }.forEach(notifications::scheduleReminder)
                }
                notifications.scheduleDebtNagging(allHabits)
                return
            }

            ACTION_DEBT_NAG -> {
                handleRescueReminder(context, localized, repository, allHabits)
                notifications.scheduleDebtNagging(allHabits)
                return
            }
        }

        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habit = allHabits.firstOrNull { it.id == habitId }
        if (habit == null || habit.isArchived) {
            notifications.scheduleDebtNagging(allHabits)
            return
        }

        val today = StreakCalculator.todayString()
        val shouldStayQuiet = !repository.areHabitRemindersEnabled() ||
            habit.isPausedOn(today) ||
            today in habit.completionHistory
        if (!shouldStayQuiet) {
            postReminder(context, localized, habit)
        }
        notifications.scheduleReminder(habit)
    }

    private fun postReminder(context: Context, localized: Context, habit: Habit) {
        val resources = localized.resources
        val title = resources.getString(R.string.notification_reminder_title, habit.name)
        val body = habit.motivationNote.takeIf { it.isNotBlank() }
            ?: resources.getString(R.string.notification_reminder_body)
        val pendingIntent = openIntent(context, habit.id, habit.id.hashCode())

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_streak)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(accentOf(habit))
            .build()
        notifySafely(context, habit.id.hashCode(), notification)
    }

    private fun handleRescueReminder(
        context: Context,
        localized: Context,
        repository: HabitRepository,
        habits: List<Habit>
    ) {
        if (!repository.areRescueRemindersEnabled()) return
        val today = StreakCalculator.todayString()
        val resources = localized.resources

        habits.filter { habit ->
            !habit.isArchived &&
                !habit.isPausedOn(today) &&
                (habit.debts.isNotEmpty() || habit.unclaimedMissedDates(today).isNotEmpty())
        }.forEach { habit ->
            val title = resources.getString(R.string.notification_rescue_title, habit.name)
            val unclaimed = habit.unclaimedMissedDates(today)
            val body = if (unclaimed.isNotEmpty()) {
                resources.getString(R.string.notification_rescue_unclaimed)
            } else {
                resources.getQuantityString(
                    R.plurals.notification_rescue_debt,
                    habit.debts.size,
                    habit.debts.size
                )
            }
            val pendingIntent = openIntent(context, habit.id, (habit.id + "_rescue").hashCode())
            val notification = NotificationCompat.Builder(context, NotificationHelper.DEBT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_streak)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(accentOf(habit))
                .build()
            notifySafely(context, (habit.id + "_rescue").hashCode(), notification)
        }
    }

    private fun openIntent(context: Context, habitId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("HABIT_ID", habitId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun accentOf(habit: Habit): Int = runCatching {
        Color.parseColor(habit.colorHex)
    }.getOrDefault(Color.parseColor("#E97B4A"))

    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission can be revoked between the check and the post; staying quiet is correct.
        }
    }
}
