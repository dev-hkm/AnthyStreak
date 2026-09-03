package com.hkm.streakwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.streakwidget.MainActivity
import com.hkm.streakwidget.data.HabitRepository
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.ui.components.CustomFlame
import com.hkm.streakwidget.ui.components.FlameState
import com.hkm.streakwidget.ui.components.LucideIcons
import com.hkm.streakwidget.ui.theme.StreakWidgetTheme

/** Appears only while a user adds or reconfigures a home-screen widget. */
class WidgetConfigurationActivity : ComponentActivity() {
    private lateinit var repository: HabitRepository
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        repository = HabitRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            val activeHabits = remember { repository.getAllHabits().filterNot(Habit::isArchived) }
            val configuredHabitId = remember { repository.getWidgetHabitId(appWidgetId) }
            val configuredStyle = remember { repository.getWidgetStyle(appWidgetId) }
            StreakWidgetTheme(mode = repository.getThemeMode()) {
                WidgetGoalPicker(
                    habits = activeHabits,
                    initiallySelectedHabitId = configuredHabitId,
                    initiallySelectedStyle = configuredStyle,
                    onDismiss = ::finish,
                    onComplete = ::completeConfiguration,
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                )
            }
        }
    }

    private fun completeConfiguration(habit: Habit, style: WidgetStyle) {
        repository.saveWidgetHabitId(appWidgetId, habit.id)
        repository.saveWidgetStyle(appWidgetId, style)
        repository.widgetActionToken(appWidgetId)
        StreakAppWidgetProvider.updateWidget(this, appWidgetId)
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        finish()
    }
}

@Composable
private fun WidgetGoalPicker(
    habits: List<Habit>,
    initiallySelectedHabitId: String?,
    initiallySelectedStyle: WidgetStyle,
    onDismiss: () -> Unit,
    onComplete: (Habit, WidgetStyle) -> Unit,
    onOpenApp: () -> Unit
) {
    var selectedHabit by remember(habits, initiallySelectedHabitId) {
        mutableStateOf(habits.firstOrNull { it.id == initiallySelectedHabitId })
    }
    var selectedStyle by remember(initiallySelectedStyle) { mutableStateOf(initiallySelectedStyle) }
    var choosingStyle by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("WIDGET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.4.sp)
                Text(
                    if (choosingStyle) "Choose its character" else "Choose a daily goal",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(LucideIcons.X, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (habits.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CustomFlame(FlameState.IDLE, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(12.dp))
                Text("Create one promise first.", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text("Your widget will be ready as soon as a goal exists.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onOpenApp, shape = RoundedCornerShape(18.dp)) {
                    Icon(LucideIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Widget", fontWeight = FontWeight.ExtraBold)
                }
            }
        } else if (!choosingStyle) {
            Text(
                if (initiallySelectedHabitId == null) {
                    "The widget follows this goal directly. You can add another widget for another goal."
                } else {
                    "Reconfigure this widget without affecting your other placements."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(habits, key = Habit::id) { habit ->
                    WidgetGoalRow(
                        habit = habit,
                        selected = habit.id == selectedHabit?.id,
                        onClick = {
                            selectedHabit = habit
                            choosingStyle = true
                        }
                    )
                }
            }
        } else {
            val habit = selectedHabit ?: return@Column
            Text(
                "${habit.name} · your saved style stays intact when the launcher resizes the widget.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(WidgetStyle.entries, key = WidgetStyle::name) { style ->
                    WidgetStylePreview(
                        style = style,
                        habit = habit,
                        selected = style == selectedStyle,
                        onClick = { selectedStyle = style }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { choosingStyle = false },
                    modifier = Modifier.weight(0.42f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Back", fontWeight = FontWeight.ExtraBold)
                }
                Button(
                    onClick = { onComplete(habit, selectedStyle) },
                    modifier = Modifier.weight(0.58f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (initiallySelectedHabitId == null) "Add widget" else "Update widget", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun WidgetGoalRow(habit: Habit, selected: Boolean, onClick: () -> Unit) {
    val today = StreakCalculator.todayString()
    val completed = today in habit.completionHistory
    val streak = StreakCalculator.calculate(habit.completionHistory, habit.debts, today)
    val flameState = when {
        completed -> FlameState.COMPLETED
        streak > 0 -> FlameState.ACTIVE
        else -> FlameState.IDLE
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomFlame(flameState, modifier = Modifier.size(48.dp), flameColor = habit.widgetAccent())
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (completed) "Today secured" else "Ready for today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(streak.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (streak == 1) "DAY" else "DAYS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(6.dp))
            Icon(LucideIcons.ChevronRight, contentDescription = "Use ${habit.name} in widget", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WidgetStylePreview(
    style: WidgetStyle,
    habit: Habit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val today = StreakCalculator.todayString()
    val streak = StreakCalculator.calculate(habit.completionHistory, habit.debts, today)
    val title = when (style) {
        WidgetStyle.EMBER -> "Ember"
        WidgetStyle.FOCUS -> "Focus"
        WidgetStyle.CHECKLIST -> "Checklist"
        WidgetStyle.POSTER -> "Poster"
    }
    val description = when (style) {
        WidgetStyle.EMBER -> "Flame and streak, distilled for every size."
        WidgetStyle.FOCUS -> "Goal, state and direct daily check-in."
        WidgetStyle.CHECKLIST -> if (habit.goalType == GoalType.CHECKLIST) {
            "Daily task progress gets the strongest supporting line."
        } else {
            "Falls back to Focus until this goal has a checklist."
        }
        WidgetStyle.POSTER -> "Oversized flame and count on Square and Hero sizes."
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CustomFlame(
                        if (today in habit.completionHistory) FlameState.COMPLETED else FlameState.ACTIVE,
                        modifier = Modifier.size(if (style == WidgetStyle.POSTER) 43.dp else 35.dp),
                        flameColor = habit.widgetAccent()
                    )
                    Text(
                        streak.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (style == WidgetStyle.POSTER) 20.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (selected) {
                Text("SELECTED", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun Habit.widgetAccent(): Color = runCatching {
    Color(android.graphics.Color.parseColor(colorHex))
}.getOrDefault(Color(0xFFE97B4A))
