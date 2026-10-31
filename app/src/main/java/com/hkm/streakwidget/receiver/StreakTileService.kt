package com.hkm.streakwidget.receiver

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.helper.NotificationHelper
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.widget.StreakAppWidgetProvider
import java.util.concurrent.Executors

/** One-tap check-in from the Quick Settings shade for the focused goal. */
class StreakTileService : TileService() {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "streak-tile").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::checkInSafely) else checkInSafely()
    }

    private fun checkInSafely() {
        executor.execute {
            val repository = HabitRepository(applicationContext)
            val today = StreakCalculator.todayString()
            val habits = repository.getAllHabits().map { it.reconciled(today) }
            val focusedId = repository.getFocusedHabitId()
            val target = habits.firstOrNull { it.id == focusedId && !it.isArchived }
                ?: habits.firstOrNull { !it.isArchived }

            if (target == null) {
                mainHandler.post(::openApp)
                return@execute
            }
            if (target.isPausedOn(today) || today in target.completionHistory) {
                mainHandler.post(::refreshTile)
                return@execute
            }

            val updated = target.copy(
                completionHistory = target.completionHistory + today,
                debts = target.debts - today
            ).let {
                it.copy(streakCount = StreakCalculator.calculate(it.completionHistory, it.debts, today, it.pausedDates))
            }
            val next = habits.map { if (it.id == updated.id) updated else it }
            repository.saveAllHabits(next)
            NotificationHelper(applicationContext).scheduleDebtNagging(next)
            applicationContext.sendBroadcast(
                Intent(StreakAppWidgetProvider.ACTION_WIDGET_DATA_CHANGED).setPackage(packageName)
            )
            StreakAppWidgetProvider.updateAllWidgets(applicationContext)
            mainHandler.post(::refreshTile)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val repository = HabitRepository(applicationContext)
        val today = StreakCalculator.todayString()
        val active = repository.getAllHabits().filterNot(Habit::isArchived)
        val focused = active.firstOrNull { it.id == repository.getFocusedHabitId() } ?: active.firstOrNull()
        val localized = applicationContext.localizedFor(repository.getAppLanguage())

        val state = when {
            focused == null -> Tile.STATE_UNAVAILABLE
            today in focused.completionHistory -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        val description = when {
            focused == null -> localized.getString(R.string.tile_no_goal)
            focused.isPausedOn(today) -> localized.getString(R.string.tile_paused)
            today in focused.completionHistory -> localized.getString(R.string.tile_secured)
            else -> localized.getString(R.string.tile_ready)
        }
        tile.state = state
        tile.label = localized.getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.contentDescription = description
            tile.subtitle = description
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_TODAY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

private fun Habit.reconciled(today: String): Habit {
    val debts = StreakCalculator.reconcileDebts(
        startDate = startDate,
        completedDates = completionHistory,
        existingDebtDates = debts,
        today = today,
        pausedDates = pausedDates
    )
    return copy(
        debts = debts,
        streakCount = StreakCalculator.calculate(completionHistory, debts, today, pausedDates)
    )
}
