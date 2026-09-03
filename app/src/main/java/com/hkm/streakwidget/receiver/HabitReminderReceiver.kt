package com.hkm.streakwidget.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.helper.NotificationHelper

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // Handle reschedule if it's a boot complete
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = HabitRepository(context)
            val notificationHelper = NotificationHelper(context)
            repository.getAllHabits().forEach { habit ->
                if (habit.reminderEnabled) {
                    notificationHelper.scheduleReminder(habit)
                }
            }
            return
        }

        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Streak Goal"
        val habitNote = intent.getStringExtra("HABIT_NOTE") ?: "Keep your streak going!"

        // Trigger notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("HABIT_ID", habitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System alarm icon as default small icon
            .setContentTitle("Streak Alert: $habitName")
            .setContentText(habitNote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(habitNote))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF5722.toInt()) // Accent Color (Flame Orange)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)

        // Reschedule alarm for tomorrow since it was a one-shot alarm to be reliable
        val repository = HabitRepository(context)
        val habit = repository.getAllHabits().find { it.id == habitId }
        if (habit != null && habit.reminderEnabled) {
            NotificationHelper(context).scheduleReminder(habit)
        }
    }
}
