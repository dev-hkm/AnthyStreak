package com.hkm.streakwidget.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class GoalType {
    GOAL,
    CHECKLIST
}

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completedOn: String? = null
) {
    fun isCompletedOn(date: String): Boolean = completedOn == date

    fun toggleFor(date: String): TodoItem =
        copy(completedOn = if (isCompletedOn(date)) null else date)

    fun toJsonObject(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("completedOn", completedOn ?: JSONObject.NULL)

    companion object {
        fun fromJsonObject(json: JSONObject): TodoItem = TodoItem(
            id = json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
            title = json.optString("title").trim(),
            completedOn = json.optString("completedOn").takeIf(String::isNotBlank)
        )
    }
}

data class StreakNote(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject = JSONObject()
        .put("id", id)
        .put("date", date)
        .put("text", text)
        .put("createdAt", createdAt)

    companion object {
        fun fromJsonObject(json: JSONObject): StreakNote = StreakNote(
            id = json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
            date = json.optString("date"),
            text = json.optString("text").trim(),
            createdAt = json.optLong("createdAt", 0L)
        )
    }
}

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val motivationNote: String = "",
    val startDate: String, // YYYY-MM-DD
    val targetDate: String? = null, // YYYY-MM-DD
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null, // HH:mm
    val colorHex: String = "#FF5722",
    val theme: VisualTheme = VisualTheme.FLAME,
    val streakCount: Int = 0,
    val completionHistory: List<String> = emptyList(), // List of YYYY-MM-DD
    val debts: List<String> = emptyList(), // List of YYYY-MM-DD
    val notes: List<StreakNote> = emptyList(),
    val isArchived: Boolean = false,
    val goalType: GoalType = GoalType.GOAL,
    val todoItems: List<TodoItem> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("description", description)
        json.put("motivationNote", motivationNote)
        json.put("startDate", startDate)
        json.put("targetDate", targetDate ?: JSONObject.NULL)
        json.put("reminderEnabled", reminderEnabled)
        json.put("reminderTime", reminderTime ?: JSONObject.NULL)
        json.put("colorHex", colorHex)
        json.put("theme", theme.name)
        json.put("streakCount", streakCount)
        
        val historyArray = JSONArray()
        completionHistory.forEach { historyArray.put(it) }
        json.put("completionHistory", historyArray)
        
        val debtsArray = JSONArray()
        debts.forEach { debtsArray.put(it) }
        json.put("debts", debtsArray)

        val notesArray = JSONArray()
        notes.forEach { notesArray.put(it.toJsonObject()) }
        json.put("notes", notesArray)
        
        json.put("isArchived", isArchived)
        json.put("goalType", goalType.name)
        val todosArray = JSONArray()
        todoItems.forEach { todosArray.put(it.toJsonObject()) }
        json.put("todoItems", todosArray)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Habit {
            val historyList = mutableListOf<String>()
            val historyArray = json.optJSONArray("completionHistory")
            if (historyArray != null) {
                for (i in 0 until historyArray.length()) {
                    historyList.add(historyArray.getString(i))
                }
            }
            
            val debtsList = mutableListOf<String>()
            val debtsArray = json.optJSONArray("debts")
            if (debtsArray != null) {
                for (i in 0 until debtsArray.length()) {
                    debtsList.add(debtsArray.getString(i))
                }
            }

            val notes = buildList {
                val notesArray = json.optJSONArray("notes") ?: return@buildList
                for (index in 0 until notesArray.length()) {
                    notesArray.optJSONObject(index)
                        ?.let(StreakNote::fromJsonObject)
                        ?.takeIf { it.text.isNotBlank() && it.date.isNotBlank() }
                        ?.let(::add)
                }
            }

            val themeName = json.optString("theme", VisualTheme.FLAME.name)
            val theme = try {
                VisualTheme.valueOf(themeName)
            } catch (e: Exception) {
                VisualTheme.FLAME
            }

            val goalType = runCatching {
                GoalType.valueOf(json.optString("goalType", GoalType.GOAL.name))
            }.getOrDefault(GoalType.GOAL)
            val todoItems = buildList {
                val todosArray = json.optJSONArray("todoItems") ?: return@buildList
                for (index in 0 until todosArray.length()) {
                    todosArray.optJSONObject(index)
                        ?.let(TodoItem::fromJsonObject)
                        ?.takeIf { it.title.isNotBlank() }
                        ?.let(::add)
                }
            }

            return Habit(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description", ""),
                motivationNote = json.optString("motivationNote", ""),
                startDate = json.getString("startDate"),
                targetDate = json.optString("targetDate").takeIf(String::isNotBlank),
                reminderEnabled = json.optBoolean("reminderEnabled", false),
                reminderTime = json.optString("reminderTime").takeIf(String::isNotBlank),
                colorHex = json.optString("colorHex", "#FF5722"),
                theme = theme,
                streakCount = json.optInt("streakCount", 0),
                completionHistory = historyList,
                debts = debtsList,
                notes = notes,
                isArchived = json.optBoolean("isArchived", false),
                goalType = goalType,
                todoItems = todoItems
            )
        }
    }
}
