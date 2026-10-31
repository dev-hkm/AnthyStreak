package com.hkm.streakwidget.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.streakwidget.data.AppLanguage
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.domain.MilestoneRules
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.helper.NotificationHelper
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.StreakNote
import com.hkm.streakwidget.model.TodoItem
import com.hkm.streakwidget.model.VisualTheme
import com.hkm.streakwidget.widget.StreakAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class GoalCheckInState(
    val habits: List<Habit>,
    val focusedHabitId: String?
) {
    fun toggleToday(habitId: String, today: String): GoalCheckInState {
        if (habits.none { it.id == habitId }) return this
        return copy(
            habits = habits.map { habit ->
                if (habit.id != habitId) {
                    habit
                } else {
                    val history = if (today in habit.completionHistory) {
                        habit.completionHistory - today
                    } else {
                        habit.completionHistory + today
                    }
                    habit.copy(
                        completionHistory = history,
                        debts = habit.debts - today
                    )
                }
            }
        )
    }
}

/** Owns the user's local goals and keeps the app widget in lock-step with them. */
class StreakViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HabitRepository(application)
    private val notifications = NotificationHelper(application)

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()
    private val _focusedHabit = MutableStateFlow<Habit?>(null)
    val focusedHabit: StateFlow<Habit?> = _focusedHabit.asStateFlow()
    private var requestedFocusId: String? = null

    private val _milestone = MutableStateFlow<MilestoneEvent?>(null)
    val milestone: StateFlow<MilestoneEvent?> = _milestone.asStateFlow()
    private val _pendingScreen = MutableStateFlow<AppScreen?>(null)
    val pendingScreen: StateFlow<AppScreen?> = _pendingScreen.asStateFlow()

    val themeMode = mutableStateOf(repository.getThemeMode())
    val hapticEnabled = mutableStateOf(repository.isHapticEnabled())
    val appLanguage = mutableStateOf(repository.getAppLanguage())
    val reduceMotion = mutableStateOf(repository.isReduceMotionEnabled())
    val remindersEnabled = mutableStateOf(repository.areHabitRemindersEnabled())
    val rescueRemindersEnabled = mutableStateOf(repository.areRescueRemindersEnabled())

    init { loadData() }

    fun loadData() = viewModelScope.launch {
        val fresh = repository.getAllHabits().map(::refreshStreak)
        _habits.value = fresh
        repository.saveAllHabits(fresh)
        val storedId = requestedFocusId ?: repository.getFocusedHabitId()
        _focusedHabit.value = fresh.firstOrNull { it.id == storedId && !it.isArchived }
            ?: fresh.firstOrNull { !it.isArchived }
        _focusedHabit.value?.let { repository.saveFocusedHabitId(it.id) }
        fresh.filter { it.reminderEnabled }.forEach(notifications::scheduleReminder)
        notifications.scheduleDebtNagging(fresh)
        refreshWidget()
    }

    fun createHabit(
        name: String,
        motivationNote: String,
        colorHex: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        goalType: GoalType = GoalType.GOAL,
        todoItems: List<TodoItem> = emptyList(),
        theme: VisualTheme = VisualTheme.FLAME
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val habit = Habit(
            name = cleanName,
            motivationNote = motivationNote.trim(),
            startDate = StreakCalculator.todayString(),
            colorHex = colorHex,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime?.takeIf { it.isNotBlank() },
            theme = theme,
            goalType = goalType,
            todoItems = if (goalType == GoalType.CHECKLIST) todoItems.filter { it.title.isNotBlank() } else emptyList()
        )
        persist(_habits.value + habit, habit.id)
        if (habit.reminderEnabled) notifications.scheduleReminder(habit)
    }

    fun updateFocusedHabit(
        name: String,
        motivationNote: String,
        colorHex: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        goalType: GoalType = GoalType.GOAL,
        todoItems: List<TodoItem> = emptyList(),
        theme: VisualTheme = VisualTheme.FLAME
    ) {
        val current = _focusedHabit.value ?: return
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        notifications.cancelReminder(current)
        val updated = refreshStreak(current.copy(
            name = cleanName,
            motivationNote = motivationNote.trim(),
            colorHex = colorHex,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime?.takeIf { it.isNotBlank() },
            theme = theme,
            goalType = goalType,
            todoItems = if (goalType == GoalType.CHECKLIST) todoItems.filter { it.title.isNotBlank() } else emptyList()
        ))
        persist(_habits.value.map { if (it.id == updated.id) updated else it }, updated.id)
        if (updated.reminderEnabled) notifications.scheduleReminder(updated)
    }

    fun setFocusHabit(habit: Habit) {
        if (habit.isArchived) return
        repository.saveFocusedHabitId(habit.id)
        _focusedHabit.value = habit
        refreshWidget()
    }

    fun focusHabitById(id: String) {
        requestedFocusId = id
        _habits.value.firstOrNull { it.id == id && !it.isArchived }?.let(::setFocusHabit)
    }

    fun toggleToday(habitId: String) {
        val today = StreakCalculator.todayString()
        val before = _habits.value.firstOrNull { it.id == habitId } ?: return
        val previousStreak = StreakCalculator.calculate(
            before.completionHistory,
            before.debts,
            today,
            before.pausedDates
        )
        val changed = GoalCheckInState(
            habits = _habits.value,
            focusedHabitId = _focusedHabit.value?.id
        ).toggleToday(habitId, today)
        if (changed.habits === _habits.value) return
        val refreshed = changed.habits.map { habit ->
            if (habit.id == habitId) refreshStreak(habit) else habit
        }
        persist(refreshed, changed.focusedHabitId)
        val after = refreshed.firstOrNull { it.id == habitId } ?: return
        MilestoneRules.crossed(previousStreak, after.streakCount)?.let { days ->
            _milestone.value = MilestoneEvent(habitId = after.id, habitName = after.name, days = days)
        }
    }

    fun consumeMilestone() {
        _milestone.value = null
    }

    fun togglePauseToday() {
        val habit = _focusedHabit.value ?: return
        val today = StreakCalculator.todayString()
        val pausedDates = if (habit.isPausedOn(today)) {
            habit.pausedDates - today
        } else {
            (habit.pausedDates + today).distinct().sorted()
        }
        val updated = refreshStreak(habit.copy(pausedDates = pausedDates, debts = habit.debts - today))
        persist(_habits.value.map { if (it.id == habit.id) updated else it }, habit.id)
    }

    /** One-tap check-in used by the Quick Settings tile; never toggles off. */
    fun checkInFocusedToday(): Boolean {
        val today = StreakCalculator.todayString()
        val habit = _focusedHabit.value
            ?: _habits.value.firstOrNull { !it.isArchived }
            ?: return false
        if (habit.isPausedOn(today)) return false
        if (today in habit.completionHistory) return true
        toggleToday(habit.id)
        return true
    }

    fun localizedContext(): Context = getApplication<Application>().localizedFor(appLanguage.value)

    fun requestScreen(screen: AppScreen) {
        _pendingScreen.value = screen
    }

    fun consumeRequestedScreen() {
        _pendingScreen.value = null
    }

    fun addNote(text: String, date: String = StreakCalculator.todayString()) {
        val habit = _focusedHabit.value ?: return
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val updated = habit.copy(notes = habit.notes + StreakNote(date = date, text = cleanText))
        persist(_habits.value.map { if (it.id == habit.id) updated else it }, habit.id)
    }

    fun deleteNote(noteId: String) {
        val habit = _focusedHabit.value ?: return
        val updated = habit.copy(notes = habit.notes.filterNot { it.id == noteId })
        persist(_habits.value.map { if (it.id == habit.id) updated else it }, habit.id)
    }

    fun toggleTodoToday(todoId: String) {
        val habit = _focusedHabit.value ?: return
        val today = StreakCalculator.todayString()
        val updated = habit.copy(
            todoItems = habit.todoItems.map { item ->
                if (item.id == todoId) item.toggleFor(today) else item
            }
        )
        persist(_habits.value.map { if (it.id == habit.id) updated else it }, updated.id)
    }

    fun archiveFocusedHabit() {
        val habit = _focusedHabit.value ?: return
        notifications.cancelReminder(habit)
        val next = _habits.value.map { if (it.id == habit.id) it.copy(isArchived = true) else it }
        persist(next, next.firstOrNull { !it.isArchived }?.id)
    }

    fun restoreHabit(habit: Habit) {
        val next = _habits.value.map { if (it.id == habit.id) refreshStreak(it.copy(isArchived = false)) else it }
        persist(next, habit.id)
    }

    fun deleteFocusedHabit() {
        val habit = _focusedHabit.value ?: return
        notifications.cancelReminder(habit)
        val next = _habits.value.filterNot { it.id == habit.id }
        persist(next, next.firstOrNull { !it.isArchived }?.id)
    }

    fun updateThemeMode(mode: String) {
        themeMode.value = mode
        repository.setThemeMode(mode)
        refreshWidget()
    }

    fun updateHaptics(enabled: Boolean) {
        hapticEnabled.value = enabled
        repository.setHapticEnabled(enabled)
    }

    fun updateAppLanguage(language: AppLanguage) {
        appLanguage.value = language
        repository.setAppLanguage(language)
        rescheduleNotifications()
        refreshWidget()
    }

    fun updateReduceMotion(enabled: Boolean) {
        reduceMotion.value = enabled
        repository.setReduceMotionEnabled(enabled)
    }

    fun updateRemindersEnabled(enabled: Boolean) {
        remindersEnabled.value = enabled
        repository.setHabitRemindersEnabled(enabled)
        if (enabled) {
            _habits.value.filter { it.reminderEnabled }.forEach(notifications::scheduleReminder)
        } else {
            _habits.value.forEach(notifications::cancelReminder)
        }
    }

    fun updateRescueRemindersEnabled(enabled: Boolean) {
        rescueRemindersEnabled.value = enabled
        repository.setRescueRemindersEnabled(enabled)
        notifications.scheduleDebtNagging(_habits.value)
    }

    fun exportBackup(): String = repository.exportBackupJson()

    fun importBackup(rawJson: String): Boolean {
        _habits.value.forEach(notifications::cancelReminder)
        val restored = repository.importBackupJson(rawJson)
        if (restored) {
            syncSettingsFromRepository()
            loadData()
        }
        return restored
    }

    fun resetAllData() {
        _habits.value.forEach(notifications::cancelReminder)
        repository.resetAllData()
        syncSettingsFromRepository()
        loadData()
    }

    private fun syncSettingsFromRepository() {
        themeMode.value = repository.getThemeMode()
        hapticEnabled.value = repository.isHapticEnabled()
        appLanguage.value = repository.getAppLanguage()
        reduceMotion.value = repository.isReduceMotionEnabled()
        remindersEnabled.value = repository.areHabitRemindersEnabled()
        rescueRemindersEnabled.value = repository.areRescueRemindersEnabled()
    }

    fun claimDebt(habitId: String, datesToClaim: List<String>) {
        val habit = _habits.value.firstOrNull { it.id == habitId } ?: return
        val updatedDebts = (habit.debts + datesToClaim).distinct().sorted()
        val updated = habit.copy(
            debts = updatedDebts,
            streakCount = StreakCalculator.calculate(
                habit.completionHistory,
                updatedDebts,
                pausedDates = habit.pausedDates
            )
        )
        val next = _habits.value.map { if (it.id == habitId) updated else it }
        persist(next, habitId)
    }

    fun payDebt(habitId: String, debtDate: String) {
        val habit = _habits.value.firstOrNull { it.id == habitId } ?: return
        val updatedDebts = habit.debts - debtDate
        val updatedHistory = (habit.completionHistory + debtDate).distinct().sorted()
        val updated = habit.copy(
            completionHistory = updatedHistory,
            debts = updatedDebts,
            streakCount = StreakCalculator.calculate(
                updatedHistory,
                updatedDebts,
                pausedDates = habit.pausedDates
            )
        )
        val next = _habits.value.map { if (it.id == habitId) updated else it }
        persist(next, habitId)
    }

    private fun refreshStreak(habit: Habit): Habit {
        val reconciledDebts = StreakCalculator.reconcileDebts(
            startDate = habit.startDate,
            completedDates = habit.completionHistory,
            existingDebtDates = habit.debts,
            pausedDates = habit.pausedDates
        )
        return habit.copy(
            debts = reconciledDebts,
            streakCount = StreakCalculator.calculate(
                habit.completionHistory,
                reconciledDebts,
                pausedDates = habit.pausedDates
            )
        )
    }

    private fun rescheduleNotifications() {
        _habits.value.forEach(notifications::cancelReminder)
        if (remindersEnabled.value) {
            _habits.value.filter { it.reminderEnabled }.forEach(notifications::scheduleReminder)
        }
        notifications.scheduleDebtNagging(_habits.value)
    }

    private fun persist(next: List<Habit>, selectedId: String?) {
        _habits.value = next
        repository.saveAllHabits(next)
        repository.saveFocusedHabitId(selectedId)
        _focusedHabit.value = next.firstOrNull { it.id == selectedId && !it.isArchived }
        notifications.scheduleDebtNagging(next)
        refreshWidget()
    }

    private fun refreshWidget() = StreakAppWidgetProvider.updateAllWidgets(getApplication())
}
