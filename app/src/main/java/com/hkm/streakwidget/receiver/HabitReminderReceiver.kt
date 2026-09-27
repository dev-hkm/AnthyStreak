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
import com.hkm.streakwidget.model.Habit

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HABIT_REMINDER = "com.hkm.streakwidget.receiver.HABIT_REMINDER"
        const val ACTION_DEBT_NAG = "com.hkm.streakwidget.receiver.DEBT_NAG"

        private val THREAT_MESSAGES = listOf(
            "Bạn đang nợ %d ngày streak! Lửa sắp tắt vĩnh viễn rồi, mau vào trả nợ ngay hôm nay kẻo mất hết công sức!",
            "Đừng để chuỗi streak biến mất! Bạn còn %d món nợ chưa trả. Vào app hoàn thành ngay!",
            "Hôm nay mà không trả nợ, toàn bộ chuỗi ngày cố gắng sẽ về 0! Trả nợ streak ngay lập tức!",
            "Bạn đã cam kết không bỏ cuộc mà? Còn %d ngày nợ đang chờ bạn thanh toán hôm nay!"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val repository = HabitRepository(context)
        val notificationHelper = NotificationHelper(context)
        val allHabits = repository.getAllHabits()

        // Handle reschedule if device rebooted
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            allHabits.forEach { habit ->
                if (habit.reminderEnabled) {
                    notificationHelper.scheduleReminder(habit)
                }
            }
            notificationHelper.scheduleDebtNagging(allHabits)
            return
        }

        if (action == ACTION_DEBT_NAG) {
            handleDebtNag(context, allHabits)
            notificationHelper.scheduleDebtNagging(allHabits)
            return
        }

        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habit = allHabits.firstOrNull { it.id == habitId }

        val habitName = habit?.name ?: intent.getStringExtra("HABIT_NAME") ?: "Streak Goal"
        val habitNote = habit?.motivationNote?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra("HABIT_NOTE")
            ?: "Keep your streak going!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("HABIT_ID", habitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasDebts = habit != null && habit.debts.isNotEmpty()
        val channelId = if (hasDebts) NotificationHelper.DEBT_CHANNEL_ID else NotificationHelper.CHANNEL_ID

        val title = if (hasDebts) {
            "🚨 CẢNH BÁO NỢ: $habitName"
        } else {
            "Streak Alert: $habitName"
        }

        val body = if (hasDebts) {
            val template = THREAT_MESSAGES[habit.debts.size % THREAT_MESSAGES.size]
            String.format(template, habit.debts.size)
        } else {
            habitNote
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (hasDebts) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF5722.toInt())
            .build()

        notificationManager.notify(habitId.hashCode(), notification)

        // Reschedule alarm for tomorrow
        if (habit != null && habit.reminderEnabled) {
            notificationHelper.scheduleReminder(habit)
        }
    }

    private fun handleDebtNag(context: Context, habits: List<Habit>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val inDebtHabits = habits.filter { !it.isArchived && (it.debts.isNotEmpty() || it.unclaimedMissedDates().isNotEmpty()) }

        inDebtHabits.forEachIndexed { index, habit ->
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("HABIT_ID", habit.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (habit.id + "_nag").hashCode(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val unclaimed = habit.unclaimedMissedDates()
            val (title, text) = when {
                unclaimed.isNotEmpty() -> {
                    "💥 NGUY CƠ ĐỨT STREAK: ${habit.name}" to
                        "Bạn đã lỡ ${unclaimed.size} ngày check-in và hết lượt nợ tự động! Vào app xin nợ & cam kết ngay để cứu streak!"
                }
                else -> {
                    val count = habit.debts.size
                    val msg = String.format(THREAT_MESSAGES[(index + count) % THREAT_MESSAGES.size], count)
                    "⚠️ HĂM DỌA NỢ STREAK: ${habit.name}" to msg
                }
            }

            val notification = NotificationCompat.Builder(context, NotificationHelper.DEBT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(0xFFFF5722.toInt())
                .build()

            notificationManager.notify((habit.id + "_debt").hashCode(), notification)
        }
    }
}
