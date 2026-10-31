package com.hkm.streakwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.Habit
import java.util.concurrent.Executors

/** Responsive, per-goal launcher widget with a finite best-effort check-in ritual. */
class StreakAppWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK_IN -> {
                val pendingResult = goAsync()
                WIDGET_ACTION_EXECUTOR.execute {
                    try {
                        completeTodayFromWidget(
                            context = context.applicationContext,
                            widgetId = intent.getIntExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID
                            ),
                            habitId = intent.getStringExtra(EXTRA_HABIT_ID),
                            token = intent.getStringExtra(EXTRA_WIDGET_TOKEN)
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }

            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                updateAllWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateWidgets(context, manager, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        updateWidgets(context, manager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repository = HabitRepository(context)
        appWidgetIds.forEach(repository::removeWidgetState)
        super.onDeleted(context, appWidgetIds)
    }

    private fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return

        val repository = HabitRepository(context)
        val localizedContext = context.localizedFor(repository.getAppLanguage())
        val labels = localizedWidgetLabels(localizedContext.resources)
        val storedHabits = repository.getAllHabits()
        val habits = storedHabits.map(Habit::reconciledForToday)
        if (habits != storedHabits) repository.saveAllHabits(habits)
        val focusedHabit = WidgetRenderStateFactory.selectHabit(habits, repository.getFocusedHabitId())

        ids.forEach { widgetId ->
            val mappedHabit = repository.getWidgetHabitId(widgetId)
                ?.let { mappedId -> habits.firstOrNull { it.id == mappedId && !it.isArchived } }
            val habit = mappedHabit ?: focusedHabit

            if (habit != null && mappedHabit == null) repository.saveWidgetHabitId(widgetId, habit.id)

            val state = WidgetRenderStateFactory.create(habit, StreakCalculator.todayString(), labels)
            val views = createResponsiveViews(
                context = context,
                localizedContext = localizedContext,
                labels = labels,
                widgetId = widgetId,
                options = manager.getAppWidgetOptions(widgetId),
                habit = habit,
                state = state,
                selectedStyle = repository.getWidgetStyle(widgetId),
                repository = repository
            )
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun createResponsiveViews(
        context: Context,
        localizedContext: Context,
        labels: WidgetLabels,
        widgetId: Int,
        options: Bundle,
        habit: Habit?,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle,
        repository: HabitRepository
    ): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val variants = linkedMapOf<SizeF, RemoteViews>()
            WidgetLayoutSpec.responsiveSizes.forEach { size ->
                variants[SizeF(size.minWidthDp.toFloat(), size.minHeightDp.toFloat())] =
                    createViewsForSize(localizedContext, widgetId, size, habit, state, selectedStyle, repository)
            }
            return RemoteViews(variants)
        }

        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return createViewsForSize(
            localizedContext,
            widgetId,
            WidgetLayoutSpec.classify(width, height),
            habit,
            state,
            selectedStyle,
            repository
        )
    }

    private fun createViewsForSize(
        context: Context,
        widgetId: Int,
        size: WidgetSize,
        habit: Habit?,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle,
        repository: HabitRepository
    ): RemoteViews {
        val style = resolvedStyle(
            size = size,
            selected = selectedStyle,
            hasChecklist = habit?.isWidgetChecklistEligible() == true
        )
        val views = RemoteViews(context.packageName, size.layoutRes())
        val hasLitFlame = state.visualState != WidgetVisualState.EMPTY && (state.streak > 0 || state.isCompleted)
        if (hasLitFlame) {
            val accent = Color.parseColor(state.accentHex)
            views.setInt(R.id.widget_flame_glow, "setColorFilter", accent)
            views.setInt(R.id.widget_flame_outer, "setColorFilter", accent)
            views.setInt(R.id.widget_flame_mid, "setColorFilter", lightenedAccent(accent))
            views.setInt(R.id.widget_flame_inner, "setColorFilter", Color.WHITE)
        }

        views.setTextViewText(
            R.id.widget_eyebrow,
            context.getString(if (habit == null) R.string.widget_eyebrow_empty else R.string.widget_eyebrow_today)
        )
        views.setTextViewText(R.id.widget_streak_count, state.streak.toString())
        views.setTextViewText(R.id.widget_streak_label, state.streakLabel)
        views.setTextViewText(R.id.widget_habit_name, state.title)
        views.setTextViewText(R.id.widget_status, state.status)
        views.setTextViewText(R.id.widget_task_progress, state.taskProgress ?: "")
        views.setTextViewText(R.id.widget_check_in, state.actionLabel)

        val showsDetail = style == WidgetStyle.FOCUS || style == WidgetStyle.CHECKLIST
        val showsEyebrow = showsDetail && size != WidgetSize.COMPACT && size != WidgetSize.SQUARE
        val showsProgress = style == WidgetStyle.CHECKLIST && state.taskProgress != null
        val showsStatus = showsDetail && !showsProgress && size != WidgetSize.COMPACT
        val showsAction = style != WidgetStyle.EMBER
        views.setViewVisibility(R.id.widget_eyebrow, if (showsEyebrow) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_habit_name, if (showsDetail) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_status, if (showsStatus) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_task_progress, if (showsProgress) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_check_in, if (showsAction) View.VISIBLE else View.GONE)

        val openIntent = openAppIntent(context, widgetId, habit?.id)
        val primaryIntent = if (state.primaryAction == WidgetPrimaryAction.CHECK_IN && habit != null) {
            checkInIntent(context, repository, widgetId, habit.id)
        } else {
            openIntent
        }
        views.setOnClickPendingIntent(R.id.widget_background, openIntent)
        views.setOnClickPendingIntent(R.id.widget_check_in, primaryIntent)
        views.setOnClickPendingIntent(R.id.widget_flame_outer, primaryIntent)
        views.setOnClickPendingIntent(R.id.widget_streak_count, primaryIntent)

        val description = if (habit == null) {
            context.getString(R.string.widget_cd_empty)
        } else {
            context.getString(R.string.widget_cd_summary, habit.name, state.streak, state.streakLabel)
        }
        views.setContentDescription(R.id.widget_background, description)
        views.setContentDescription(R.id.widget_flame_outer, state.actionLabel)
        views.setContentDescription(R.id.widget_check_in, state.actionLabel)
        return views
    }

    private fun completeTodayFromWidget(context: Context, widgetId: Int, habitId: String?, token: String?) {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || habitId.isNullOrBlank()) return

        val repository = HabitRepository(context)
        if (!repository.isValidWidgetAction(widgetId, habitId, token)) return

        val localizedContext = context.localizedFor(repository.getAppLanguage())
        val labels = localizedWidgetLabels(localizedContext.resources)
        val today = StreakCalculator.todayString()
        val storedHabits = repository.getAllHabits()
        val habits = storedHabits.map(Habit::reconciledForToday)
        if (habits != storedHabits) repository.saveAllHabits(habits)
        val target = habits.firstOrNull { it.id == habitId && !it.isArchived } ?: return
        if (today in target.completionHistory || target.isPausedOn(today)) {
            updateAllWidgets(context)
            return
        }

        val completedHistory = target.completionHistory + today
        val remainingDebts = target.debts - today
        val updatedTarget = target.copy(
            completionHistory = completedHistory,
            debts = remainingDebts,
            streakCount = StreakCalculator.calculate(
                completedHistory,
                remainingDebts,
                today,
                target.pausedDates
            )
        )
        repository.saveAllHabits(habits.map { if (it.id == habitId) updatedTarget else it })
        context.sendBroadcast(Intent(ACTION_WIDGET_DATA_CHANGED).setPackage(context.packageName))

        val settledState = WidgetRenderStateFactory.create(updatedTarget, today, labels)
        try {
            playIgnitionFrames(context, widgetId, settledState)
        } finally {
            updateAllWidgets(context)
        }
    }

    private fun playIgnitionFrames(context: Context, widgetId: Int, state: WidgetRenderState) {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(widgetId)
        val size = WidgetLayoutSpec.classify(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        )
        val accent = Color.parseColor(state.accentHex)
        var elapsed = 0L
        for (frame in WidgetIgnitionSequence.frames()) {
            val waitMillis = frame.atMillis - elapsed
            if (waitMillis > 0L) Thread.sleep(waitMillis)
            elapsed = frame.atMillis

            val partial = RemoteViews(context.packageName, size.layoutRes()).apply {
                setInt(R.id.widget_flame_glow, "setColorFilter", accent)
                setInt(R.id.widget_flame_outer, "setColorFilter", accent)
                setInt(R.id.widget_flame_mid, "setColorFilter", lightenedAccent(accent))
                setInt(R.id.widget_flame_inner, "setColorFilter", Color.WHITE)
                setInt(R.id.widget_flame_glow, "setImageAlpha", frame.glowAlpha)
                setInt(R.id.widget_flame_outer, "setImageAlpha", frame.outerAlpha)
                setInt(R.id.widget_flame_mid, "setImageAlpha", frame.midAlpha)
                setInt(R.id.widget_flame_inner, "setImageAlpha", frame.innerAlpha)
                setTextViewText(R.id.widget_streak_count, state.streak.toString())
                setTextViewText(R.id.widget_streak_label, state.streakLabel)
                setTextViewText(R.id.widget_status, state.status)
                setTextViewText(R.id.widget_check_in, state.actionLabel)
            }
            manager.partiallyUpdateAppWidget(widgetId, partial)
        }
    }

    private fun openAppIntent(context: Context, widgetId: Int, habitId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_APP
            data = Uri.parse("streakwidget://open/$widgetId/${habitId ?: "new"}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            habitId?.let { putExtra(EXTRA_HABIT_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun checkInIntent(
        context: Context,
        repository: HabitRepository,
        widgetId: Int,
        habitId: String
    ): PendingIntent {
        val intent = Intent(context, StreakAppWidgetProvider::class.java).apply {
            action = ACTION_CHECK_IN
            data = Uri.parse("streakwidget://check-in/$widgetId/$habitId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_WIDGET_TOKEN, repository.widgetActionToken(widgetId))
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_CHECK_IN = "com.hkm.streakwidget.widget.CHECK_IN"
        const val ACTION_OPEN_APP = "com.hkm.streakwidget.widget.OPEN_APP"
        const val ACTION_WIDGET_DATA_CHANGED = "com.hkm.streakwidget.widget.DATA_CHANGED"
        const val EXTRA_HABIT_ID = "HABIT_ID"
        const val EXTRA_WIDGET_TOKEN = "WIDGET_TOKEN"

        private val WIDGET_ACTION_EXECUTOR = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "widget-check-in").apply { isDaemon = true }
        }

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, StreakAppWidgetProvider::class.java)
            StreakAppWidgetProvider().updateWidgets(appContext, manager, manager.getAppWidgetIds(component))
        }

        fun updateWidget(context: Context, widgetId: Int) {
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val appContext = context.applicationContext
            StreakAppWidgetProvider().updateWidgets(
                appContext,
                AppWidgetManager.getInstance(appContext),
                intArrayOf(widgetId)
            )
        }
    }
}

/** Mixes an accent toward white so the widget mid-flame reads as hot glow. */
private fun lightenedAccent(accent: Int): Int {
    fun mix(channel: Int): Int = (channel + (255 - channel) * 0.45f).toInt().coerceIn(0, 255)
    return Color.rgb(
        mix(Color.red(accent)),
        mix(Color.green(accent)),
        mix(Color.blue(accent))
    )
}

private fun Habit.reconciledForToday(today: String = StreakCalculator.todayString()): Habit {
    val reconciledDebts = StreakCalculator.reconcileDebts(
        startDate = startDate,
        completedDates = completionHistory,
        existingDebtDates = debts,
        today = today,
        pausedDates = pausedDates
    )
    return copy(
        debts = reconciledDebts,
        streakCount = StreakCalculator.calculate(completionHistory, reconciledDebts, today, pausedDates)
    )
}

private fun localizedWidgetLabels(resources: Resources): WidgetLabels = WidgetLabels(
    emptyTitle = resources.getString(R.string.widget_empty_title),
    emptyStatus = resources.getString(R.string.widget_empty_status),
    createGoal = resources.getString(R.string.widget_create_goal),
    ready = resources.getString(R.string.widget_ready),
    secured = resources.getString(R.string.widget_secured),
    checkIn = resources.getString(R.string.widget_check_in),
    paused = resources.getString(R.string.widget_paused),
    open = resources.getString(R.string.widget_open),
    day = resources.getQuantityString(R.plurals.widget_days, 1, 1),
    days = resources.getQuantityString(R.plurals.widget_days, 2, 2),
    taskProgress = resources.getString(R.string.widget_task_progress)
)

private fun WidgetSize.layoutRes(): Int = when (this) {
    WidgetSize.TILE -> R.layout.widget_layout_tile
    WidgetSize.STRIP -> R.layout.widget_layout_strip
    WidgetSize.COMPACT -> R.layout.widget_layout_compact
    WidgetSize.SQUARE -> R.layout.widget_layout_square
    WidgetSize.WIDE -> R.layout.widget_layout_wide
    WidgetSize.HERO -> R.layout.widget_layout_hero
}
