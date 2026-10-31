package com.hkm.streakwidget.data

import android.content.Context
import android.content.SharedPreferences
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.widget.WidgetStyle
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HabitRepository internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences("streak_widget_prefs", Context.MODE_PRIVATE)
    )

    fun getAllHabits(): List<Habit> {
        val habitsJson = prefs.getString("habits_json", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(habitsJson)
            val habitsList = mutableListOf<Habit>()
            for (i in 0 until jsonArray.length()) {
                habitsList.add(Habit.fromJsonObject(jsonArray.getJSONObject(i)))
            }
            habitsList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveAllHabits(habits: List<Habit>) {
        val jsonArray = JSONArray()
        habits.forEach { jsonArray.put(it.toJsonObject()) }
        prefs.edit().putString("habits_json", jsonArray.toString()).apply()
    }

    fun getFocusedHabitId(): String? {
        return prefs.getString("focused_habit_id", null)
    }

    fun saveFocusedHabitId(id: String?) {
        prefs.edit().putString("focused_habit_id", id).apply()
    }

    /**
     * Widget placement is device-local, so its appWidget IDs deliberately stay out of
     * portable backups. Each installed widget can point at a different active goal.
     */
    fun getWidgetHabitId(widgetId: Int): String? = prefs.getString(widgetHabitKey(widgetId), null)

    fun saveWidgetHabitId(widgetId: Int, habitId: String) {
        prefs.edit().putString(widgetHabitKey(widgetId), habitId).apply()
    }

    fun getWidgetStyle(widgetId: Int): WidgetStyle =
        WidgetStyle.fromStored(prefs.getString(widgetStyleKey(widgetId), null))

    fun saveWidgetStyle(widgetId: Int, style: WidgetStyle) {
        prefs.edit().putString(widgetStyleKey(widgetId), style.name).apply()
    }

    fun removeWidgetState(widgetId: Int) {
        prefs.edit()
            .remove(widgetHabitKey(widgetId))
            .remove(widgetStyleKey(widgetId))
            .remove(widgetTokenKey(widgetId))
            .apply()
    }

    /** An opaque token prevents forged explicit broadcasts from checking a goal in. */
    fun widgetActionToken(widgetId: Int): String {
        val key = widgetTokenKey(widgetId)
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) return existing

        val token = UUID.randomUUID().toString()
        prefs.edit().putString(key, token).apply()
        return token
    }

    fun isValidWidgetAction(widgetId: Int, habitId: String, token: String?): Boolean =
        token != null &&
            token == prefs.getString(widgetTokenKey(widgetId), null) &&
            habitId == getWidgetHabitId(widgetId)

    // App Settings
    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean("settings_haptic_enabled", true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_haptic_enabled", enabled).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString("settings_theme_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("settings_theme_mode", mode).apply()
    }

    fun getAnimationIntensity(): String {
        return prefs.getString("settings_anim_intensity", "NORMAL") ?: "NORMAL"
    }

    fun setAnimationIntensity(intensity: String) {
        prefs.edit().putString("settings_anim_intensity", intensity).apply()
    }

    fun getAppLanguage(): AppLanguage =
        AppLanguage.fromPreference(prefs.getString("settings_app_language", null))

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString("settings_app_language", language.preferenceValue).apply()
    }

    fun isReduceMotionEnabled(): Boolean = prefs.getBoolean("settings_reduce_motion", false)

    fun setReduceMotionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_reduce_motion", enabled).apply()
    }

    fun areHabitRemindersEnabled(): Boolean = prefs.getBoolean("settings_reminders_enabled", true)

    fun setHabitRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_reminders_enabled", enabled).apply()
    }

    fun areRescueRemindersEnabled(): Boolean = prefs.getBoolean("settings_rescue_enabled", true)

    fun setRescueRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_rescue_enabled", enabled).apply()
    }

    // Export JSON string for Backup
    fun exportBackupJson(): String {
        val backup = JSONObject()
        val habitsArray = JSONArray()
        getAllHabits().forEach { habitsArray.put(it.toJsonObject()) }
        backup.put("version", 3)
        backup.put("habits", habitsArray)
        backup.put("focused_habit_id", getFocusedHabitId() ?: JSONObject.NULL)
        backup.put("settings_haptic_enabled", isHapticEnabled())
        backup.put("settings_theme_mode", getThemeMode())
        backup.put("settings_anim_intensity", getAnimationIntensity())
        backup.put("settings_app_language", getAppLanguage().preferenceValue)
        backup.put("settings_reduce_motion", isReduceMotionEnabled())
        backup.put("settings_reminders_enabled", areHabitRemindersEnabled())
        backup.put("settings_rescue_enabled", areRescueRemindersEnabled())
        return backup.toString(4) // pretty print
    }

    // Import JSON string for Restore
    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val backup = JSONObject(jsonString)
            val version = backup.optInt("version", 1)
            if (version > 3) return false // unsupported newer versions
            
            val habitsArray = backup.getJSONArray("habits")
            val habitsList = mutableListOf<Habit>()
            for (i in 0 until habitsArray.length()) {
                habitsList.add(Habit.fromJsonObject(habitsArray.getJSONObject(i)))
            }
            
            saveAllHabits(habitsList)
            
            if (backup.has("focused_habit_id") && !backup.isNull("focused_habit_id")) {
                saveFocusedHabitId(backup.getString("focused_habit_id"))
            } else {
                saveFocusedHabitId(habitsList.firstOrNull()?.id)
            }
            
            if (backup.has("settings_haptic_enabled")) {
                setHapticEnabled(backup.getBoolean("settings_haptic_enabled"))
            }
            if (backup.has("settings_theme_mode")) {
                setThemeMode(backup.getString("settings_theme_mode"))
            }
            if (backup.has("settings_anim_intensity")) {
                setAnimationIntensity(backup.getString("settings_anim_intensity"))
            }
            if (backup.has("settings_app_language")) {
                setAppLanguage(AppLanguage.fromPreference(backup.optString("settings_app_language")))
            }
            if (backup.has("settings_reduce_motion")) {
                setReduceMotionEnabled(backup.getBoolean("settings_reduce_motion"))
            }
            if (backup.has("settings_reminders_enabled")) {
                setHabitRemindersEnabled(backup.getBoolean("settings_reminders_enabled"))
            }
            if (backup.has("settings_rescue_enabled")) {
                setRescueRemindersEnabled(backup.getBoolean("settings_rescue_enabled"))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetAllData() {
        prefs.edit().clear().apply()
    }

    private fun widgetHabitKey(widgetId: Int) = "widget_habit_$widgetId"
    private fun widgetStyleKey(widgetId: Int) = "widget_style_$widgetId"
    private fun widgetTokenKey(widgetId: Int) = "widget_action_token_$widgetId"
}
