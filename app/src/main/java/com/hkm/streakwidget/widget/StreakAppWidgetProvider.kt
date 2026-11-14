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
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.VisualTheme
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** Responsive, per-goal launcher widget that adapts its structure and typography to any frame. */
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
            val options = manager.getAppWidgetOptions(widgetId)
            val selectedStyle = repository.getWidgetStyle(widgetId)
            val views = createResponsiveViews(
                context = localizedContext,
                widgetId = widgetId,
                options = options,
                habit = habit,
                state = state,
                selectedStyle = selectedStyle,
                repository = repository
            )
            try {
                manager.updateAppWidget(widgetId, views)
            } catch (_: RuntimeException) {
                val fallback = createSingleFallbackViews(
                    context = localizedContext,
                    widgetId = widgetId,
                    options = options,
                    habit = habit,
                    state = state,
                    selectedStyle = selectedStyle,
                    repository = repository
                )
                manager.updateAppWidget(widgetId, fallback)
            }
        }
    }

    private fun createSingleFallbackViews(
        context: Context,
        widgetId: Int,
        options: Bundle,
        habit: Habit?,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle,
        repository: HabitRepository
    ): RemoteViews {
        val openIntent = openAppIntent(context, widgetId, habit?.id)
        val primaryIntent = if (state.primaryAction == WidgetPrimaryAction.CHECK_IN && habit != null) {
            checkInIntent(context, repository, widgetId, habit.id)
        } else {
            openIntent
        }
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return createViewsForFrame(
            context = context,
            widthDp = minWidth,
            heightDp = minHeight,
            habit = habit,
            state = state,
            selectedStyle = selectedStyle,
            openIntent = openIntent,
            primaryIntent = primaryIntent
        )
    }

    private fun createResponsiveViews(
        context: Context,
        widgetId: Int,
        options: Bundle,
        habit: Habit?,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle,
        repository: HabitRepository
    ): RemoteViews {
        val openIntent = openAppIntent(context, widgetId, habit?.id)
        val primaryIntent = if (state.primaryAction == WidgetPrimaryAction.CHECK_IN && habit != null) {
            checkInIntent(context, repository, widgetId, habit.id)
        } else {
            openIntent
        }

        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val launcherFrames = extractSizeFrames(options)
            val responsiveFrames = WidgetLayoutSpec.responsiveFrames(launcherFrames, minWidth, minHeight)
            val variants = linkedMapOf<SizeF, RemoteViews>()
            responsiveFrames.forEach { frame ->
                variants[SizeF(frame.widthDp.toFloat(), frame.heightDp.toFloat())] = createViewsForFrame(
                    context = context,
                    widthDp = frame.widthDp,
                    heightDp = frame.heightDp,
                    habit = habit,
                    state = state,
                    selectedStyle = selectedStyle,
                    openIntent = openIntent,
                    primaryIntent = primaryIntent
                )
            }
            return RemoteViews(variants)
        }

        val portraitW = if (minWidth > 0) minWidth else WidgetLayoutSpec.DEFAULT_WIDTH_DP
        val portraitH = if (maxHeight > 0) maxHeight else minHeight
        val landscapeW = if (maxWidth > 0) maxWidth else portraitW
        val landscapeH = if (minHeight > 0) minHeight else portraitH
        if (landscapeW != portraitW || landscapeH != portraitH) {
            val landscapeViews = createViewsForFrame(
                context = context,
                widthDp = landscapeW,
                heightDp = landscapeH,
                habit = habit,
                state = state,
                selectedStyle = selectedStyle,
                openIntent = openIntent,
                primaryIntent = primaryIntent
            )
            val portraitViews = createViewsForFrame(
                context = context,
                widthDp = portraitW,
                heightDp = portraitH,
                habit = habit,
                state = state,
                selectedStyle = selectedStyle,
                openIntent = openIntent,
                primaryIntent = primaryIntent
            )
            return RemoteViews(landscapeViews, portraitViews)
        }

        return createViewsForFrame(
            context = context,
            widthDp = minWidth,
            heightDp = minHeight,
            habit = habit,
            state = state,
            selectedStyle = selectedStyle,
            openIntent = openIntent,
            primaryIntent = primaryIntent
        )
    }

    @Suppress("DEPRECATION")
    private fun extractSizeFrames(options: Bundle): List<WidgetFrame> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        return options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            .orEmpty()
            .filter { it.width.isFinite() && it.height.isFinite() }
            .map { WidgetFrame(it.width.roundToInt(), it.height.roundToInt()) }
    }

    private fun createViewsForFrame(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        habit: Habit?,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle,
        openIntent: PendingIntent,
        primaryIntent: PendingIntent
    ): RemoteViews {
        val hasChecklist = habit?.isWidgetChecklistEligible() == true
        val plan = WidgetLayoutSpec.plan(
            widthDp = widthDp,
            heightDp = heightDp,
            style = selectedStyle,
            hasChecklist = hasChecklist,
            countDigits = state.streak.toString().length,
            actionCharCount = state.actionLabel.length
        )

        val views = RemoteViews(context.packageName, plan.structure.layoutRes())
        applyPlan(views, context, plan, state)

        // Match FocusCard in MainScreen.kt: the flame/bonsai lights up vividly only once today is completed.
        val lit = state.visualState != WidgetVisualState.EMPTY && state.isCompleted
        val accentColor = runCatching { Color.parseColor(state.accentHex) }.getOrDefault(0xFFFF6D00.toInt())
        val heroBitmap = WidgetFlameRenderer.composite(
            accent = accentColor,
            lit = lit,
            theme = state.theme,
            streak = state.streak
        )
        views.setImageViewBitmap(R.id.widget_flame_outer, heroBitmap)
        views.setViewVisibility(R.id.widget_flame_glow, View.GONE)
        views.setViewVisibility(R.id.widget_flame_mid, View.GONE)
        views.setViewVisibility(R.id.widget_flame_inner, View.GONE)

        val streakLabelText = if (plan.useLongStreakLabel) state.longStreakLabel else state.streakLabel
        views.setTextViewText(
            R.id.widget_eyebrow,
            context.getString(if (habit == null) R.string.widget_eyebrow_empty else R.string.widget_eyebrow_today)
        )
        views.setTextViewText(R.id.widget_streak_count, state.streak.toString())
        views.setTextViewText(R.id.widget_streak_label, streakLabelText)
        views.setTextViewText(R.id.widget_habit_name, state.title)
        views.setTextViewText(R.id.widget_status, state.status)
        views.setTextViewText(R.id.widget_task_progress, state.taskProgress ?: "")
        views.setTextViewText(R.id.widget_check_in, state.actionLabel)

        views.setOnClickPendingIntent(R.id.widget_background, openIntent)
        views.setOnClickPendingIntent(R.id.widget_check_in, primaryIntent)
        views.setOnClickPendingIntent(R.id.widget_flame_outer, primaryIntent)
        views.setOnClickPendingIntent(R.id.widget_streak_count, primaryIntent)

        val description = if (habit == null) {
            context.getString(R.string.widget_cd_empty)
        } else {
            context.getString(R.string.widget_cd_summary, habit.name, state.streak, streakLabelText)
        }
        views.setContentDescription(R.id.widget_background, description)
        views.setContentDescription(R.id.widget_flame_outer, state.actionLabel)
        views.setContentDescription(R.id.widget_check_in, state.actionLabel)
        return views
    }

    private fun applyPlan(
        views: RemoteViews,
        context: Context,
        plan: WidgetPlan,
        state: WidgetRenderState
    ) {
        val density = context.resources.displayMetrics.density
        val padPx = (plan.paddingDp * density).roundToInt()
        views.setViewPadding(R.id.widget_background, padPx, padPx, padPx, padPx)
        views.setInt(
            R.id.widget_background,
            "setBackgroundResource",
            if (plan.compactCorners) R.drawable.widget_bg_shape_compact else R.drawable.widget_bg_shape
        )

        val fontScale = context.resources.configuration.fontScale.coerceIn(1f, 1.12f)
        fun setBoundedTextSize(viewId: Int, sizeSp: Float) {
            views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_DIP, sizeSp * fontScale)
        }

        setBoundedTextSize(R.id.widget_streak_count, plan.countSp)
        setBoundedTextSize(R.id.widget_streak_label, plan.labelSp)
        setBoundedTextSize(R.id.widget_eyebrow, plan.eyebrowSp)
        setBoundedTextSize(R.id.widget_habit_name, plan.nameSp)
        setBoundedTextSize(R.id.widget_status, plan.statusSp)
        setBoundedTextSize(R.id.widget_task_progress, plan.statusSp)
        setBoundedTextSize(R.id.widget_check_in, plan.actionSp)
        views.setInt(R.id.widget_habit_name, "setMaxLines", plan.nameMaxLines)

        val isPendingCheckIn = state.primaryAction == WidgetPrimaryAction.CHECK_IN
        views.setInt(
            R.id.widget_check_in,
            "setBackgroundResource",
            if (isPendingCheckIn) R.drawable.widget_action_shape else R.drawable.widget_action_completed_shape
        )
        views.setTextColor(
            R.id.widget_check_in,
            ContextCompat.getColor(
                context,
                if (isPendingCheckIn) R.color.widget_action_text else R.color.widget_action_completed_text
            )
        )

        views.setViewVisibility(R.id.widget_streak_label, if (plan.showLabel) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_eyebrow, if (plan.showEyebrow) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_habit_name, if (plan.showName) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_status, if (plan.showStatus) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_task_progress, if (plan.showProgress) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_check_in, if (plan.showAction) View.VISIBLE else View.GONE)

        val showDetailContainer = plan.showEyebrow || plan.showName || plan.showStatus || plan.showProgress
        if (plan.structure == WidgetStructure.STRIP_ROW ||
            plan.structure == WidgetStructure.POSTER_STACK ||
            plan.structure == WidgetStructure.HERO
        ) {
            views.setViewVisibility(
                R.id.widget_detail_box,
                if (showDetailContainer) View.VISIBLE else View.GONE
            )
        }

        if (plan.structure == WidgetStructure.COLUMN) {
            val spacerVis = if (plan.useBalanceSpacers) View.VISIBLE else View.GONE
            views.setViewVisibility(R.id.widget_top_spacer, spacerVis)
            views.setViewVisibility(R.id.widget_bottom_spacer, spacerVis)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (plan.showAction) {
                views.setViewLayoutHeight(
                    R.id.widget_check_in,
                    plan.actionHeightDp.toFloat(),
                    TypedValue.COMPLEX_UNIT_DIP
                )
            }
            if (plan.structure == WidgetStructure.STRIP_ROW) {
                views.setViewLayoutWidth(
                    R.id.widget_flame_box,
                    plan.flameSizeDp.toFloat(),
                    TypedValue.COMPLEX_UNIT_DIP
                )
            }
            if (plan.structure == WidgetStructure.HERO ||
                plan.structure == WidgetStructure.COMPACT_SPLIT ||
                plan.structure == WidgetStructure.STRIP_ROW
            ) {
                views.setViewLayoutMargin(
                    R.id.widget_detail_box,
                    RemoteViews.MARGIN_START,
                    plan.gapDp.toFloat(),
                    TypedValue.COMPLEX_UNIT_DIP
                )
            }
        }
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
            playIgnitionFrames(localizedContext, widgetId, settledState, repository.getWidgetStyle(widgetId))
        } finally {
            updateAllWidgets(context)
        }
    }

    private fun playIgnitionFrames(
        context: Context,
        widgetId: Int,
        state: WidgetRenderState,
        selectedStyle: WidgetStyle
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(widgetId)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val frame = WidgetLayoutSpec.closestFrame(extractSizeFrames(options), minW, minH)
            ?: WidgetFrame(minW, minH)
        val plan = WidgetLayoutSpec.plan(
            widthDp = frame.widthDp,
            heightDp = frame.heightDp,
            style = selectedStyle,
            hasChecklist = false,
            countDigits = state.streak.toString().length,
            actionCharCount = state.actionLabel.length
        )

        val accent = runCatching { Color.parseColor(state.accentHex) }.getOrDefault(0xFFFF6D00.toInt())
        val heroBitmap = WidgetFlameRenderer.composite(
            accent = accent,
            lit = true,
            theme = state.theme,
            streak = state.streak
        )
        val labelText = if (plan.useLongStreakLabel) state.longStreakLabel else state.streakLabel
        var elapsed = 0L
        WidgetIgnitionSequence.frames().forEachIndexed { index, animFrame ->
            val waitMillis = animFrame.atMillis - elapsed
            if (waitMillis > 0L) Thread.sleep(waitMillis)
            elapsed = animFrame.atMillis

            val partial = RemoteViews(context.packageName, plan.structure.layoutRes()).apply {
                if (index == 0) {
                    setImageViewBitmap(R.id.widget_flame_outer, heroBitmap)
                    setViewVisibility(R.id.widget_flame_glow, View.GONE)
                    setViewVisibility(R.id.widget_flame_mid, View.GONE)
                    setViewVisibility(R.id.widget_flame_inner, View.GONE)
                    setTextViewText(R.id.widget_streak_count, state.streak.toString())
                    setTextViewText(R.id.widget_streak_label, labelText)
                    setTextViewText(R.id.widget_status, state.status)
                    setTextViewText(R.id.widget_check_in, state.actionLabel)
                    setInt(R.id.widget_check_in, "setBackgroundResource", R.drawable.widget_action_completed_shape)
                    setTextColor(
                        R.id.widget_check_in,
                        ContextCompat.getColor(context, R.color.widget_action_completed_text)
                    )
                }
                setInt(R.id.widget_flame_outer, "setImageAlpha", animFrame.outerAlpha)
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
    dayStreak = resources.getQuantityString(R.plurals.today_streak_label, 1),
    daysStreak = resources.getQuantityString(R.plurals.today_streak_label, 2),
    taskProgress = resources.getString(R.string.widget_task_progress)
)

private fun WidgetStructure.layoutRes(): Int = when (this) {
    WidgetStructure.MICRO_ROW -> R.layout.widget_layout_micro_row
    WidgetStructure.MICRO_STACK -> R.layout.widget_layout_micro_stack
    WidgetStructure.STRIP_ROW -> R.layout.widget_layout_strip_row
    WidgetStructure.COMPACT_SPLIT -> R.layout.widget_layout_compact_split
    WidgetStructure.COLUMN -> R.layout.widget_layout_column
    WidgetStructure.SQUARE -> R.layout.widget_layout_square
    WidgetStructure.POSTER_STACK -> R.layout.widget_layout_poster
    WidgetStructure.HERO -> R.layout.widget_layout_hero
}
