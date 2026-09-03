package com.hkm.streakwidget.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.TodoItem
import com.hkm.streakwidget.ui.StreakViewModel
import com.hkm.streakwidget.ui.components.CustomFlame
import com.hkm.streakwidget.ui.components.FlameState
import com.hkm.streakwidget.ui.components.LucideIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private enum class AppTab { TODAY, GOALS }

@Composable
fun MainScreen(viewModel: StreakViewModel) {
    val habits by viewModel.habits.collectAsState()
    val focused by viewModel.focusedHabit.collectAsState()
    val activeHabits = habits.filterNot(Habit::isArchived)
    val archivedHabits = habits.filter(Habit::isArchived)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    var tab by remember { mutableStateOf(AppTab.TODAY) }
    var editorTarget by remember { mutableStateOf<Habit?>(null) }
    var managingHabit by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showTodos by remember { mutableStateOf(false) }
    var ignitionTargetId by remember { mutableStateOf<String?>(null) }

    val tapHaptic: () -> Unit = {
        if (viewModel.hapticEnabled.value) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    val confirmHaptic: () -> Unit = {
        if (viewModel.hapticEnabled.value) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(viewModel.exportBackup())
                } ?: error("Could not create the backup file")
            }.isSuccess
            snackbar.showSnackbar(if (saved) "Backup saved" else "Could not save the backup")
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val restored = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?.let(viewModel::importBackup) == true
            }.getOrDefault(false)
            snackbar.showSnackbar(if (restored) "Backup restored" else "That JSON backup could not be restored")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            BottomTabToggle(
                selected = tab,
                onSelect = {
                    if (tab != it) {
                        tab = it
                        tapHaptic()
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.padding(scaffoldPadding),
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(tween(430, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(250)) { -it / 8 } + fadeOut(tween(180)))
                } else {
                    (slideInHorizontally(tween(430, easing = FastOutSlowInEasing)) { -it / 5 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(250)) { it / 8 } + fadeOut(tween(180)))
                }
            },
            label = "main_tab_slide"
        ) { currentTab ->
            when (currentTab) {
                AppTab.TODAY -> TodayScreen(
                    modifier = Modifier,
                    focused = focused,
                    ignitionTargetId = ignitionTargetId,
                    onToggleToday = checkIn@{ habitId ->
                        val target = habits.firstOrNull { it.id == habitId && !it.isArchived }
                            ?: return@checkIn
                        val alreadyDone = target.completionHistory.contains(StreakCalculator.todayString())
                        when {
                            ignitionTargetId != null -> Unit
                            alreadyDone -> {
                                viewModel.toggleToday(habitId)
                                confirmHaptic()
                            }
                            else -> scope.launch {
                                tapHaptic()
                                ignitionTargetId = habitId
                                delay(620)
                                viewModel.toggleToday(habitId)
                                confirmHaptic()
                                delay(1_180)
                                if (ignitionTargetId == habitId) ignitionTargetId = null
                            }
                        }
                    },
                    onHistory = {
                        showHistory = true
                        tapHaptic()
                    },
                    onOpenTodos = {
                        showTodos = true
                        tapHaptic()
                    },
                    onCreate = {
                        editorTarget = null
                        showEditor = true
                        tapHaptic()
                    },
                    onSettings = {
                        showSettings = true
                        tapHaptic()
                    }
                )

                AppTab.GOALS -> GoalsScreen(
                    modifier = Modifier,
                    active = activeHabits,
                    archived = archivedHabits,
                    onCreate = {
                        editorTarget = null
                        showEditor = true
                        tapHaptic()
                    },
                    onSelect = {
                        viewModel.setFocusHabit(it)
                        tab = AppTab.TODAY
                        tapHaptic()
                    },
                    onManage = {
                        viewModel.setFocusHabit(it)
                        managingHabit = it
                        tapHaptic()
                    }
                )
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            habit = editorTarget,
            onDismiss = { showEditor = false },
            onHaptic = tapHaptic,
            onSave = { name, note, color, reminder, time, type, todoItems ->
                if (editorTarget == null) {
                    viewModel.createHabit(name, note, color, reminder, time, type, todoItems)
                } else {
                    viewModel.updateFocusedHabit(name, note, color, reminder, time, type, todoItems)
                }
                showEditor = false
                confirmHaptic()
            }
        )
    }

    managingHabit?.let { habit ->
        GoalManageDialog(
            habit = habit,
            onDismiss = { managingHabit = null },
            onEdit = {
                editorTarget = habit
                managingHabit = null
                showEditor = true
            },
            onArchive = {
                viewModel.archiveFocusedHabit()
                managingHabit = null
                scope.launch { snackbar.showSnackbar("Goal moved to archive") }
            },
            onRestore = {
                viewModel.restoreHabit(habit)
                managingHabit = null
                scope.launch { snackbar.showSnackbar("Goal restored") }
            },
            onDelete = {
                viewModel.deleteFocusedHabit()
                managingHabit = null
                scope.launch { snackbar.showSnackbar("Goal deleted") }
            },
            onHaptic = confirmHaptic
        )
    }

    focused?.let { habit ->
        if (showHistory) {
            HistoryDialog(habit = habit, onDismiss = { showHistory = false }, onHaptic = tapHaptic)
        }
        if (showTodos && habit.goalType == GoalType.CHECKLIST && habit.todoItems.isNotEmpty()) {
            TodoDialog(
                habit = habit,
                onDismiss = { showTodos = false },
                onToggle = viewModel::toggleTodoToday,
                onHaptic = confirmHaptic
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            mode = viewModel.themeMode.value,
            haptics = viewModel.hapticEnabled.value,
            onDismiss = { showSettings = false },
            onTheme = viewModel::updateThemeMode,
            onHaptics = viewModel::updateHaptics,
            onExport = { exportBackup.launch("streak-widget-backup.json") },
            onImport = { importBackup.launch(arrayOf("application/json", "text/plain")) },
            onReset = {
                viewModel.resetAllData()
                showSettings = false
                scope.launch { snackbar.showSnackbar("All local data was reset") }
            },
            onHaptic = tapHaptic
        )
    }
}

@Composable
private fun BottomTabToggle(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
        ) {
            val segmentWidth = (maxWidth - 10.dp) / 2
            val indicatorTarget = if (selected == AppTab.TODAY) 5.dp else segmentWidth + 5.dp
            val indicatorOffset by animateDpAsState(
                targetValue = indicatorTarget,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "tab_indicator_spring"
            )
            val boundedIndicatorOffset = boundedTabIndicatorOffset(indicatorOffset, segmentWidth)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = boundedIndicatorOffset)
                    .width(segmentWidth)
                    .height(48.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MaterialTheme.colorScheme.onSurface)
            )
            Row(Modifier.fillMaxSize()) {
                BottomTabToggleItem(
                    label = "Today",
                    icon = LucideIcons.Check,
                    selected = selected == AppTab.TODAY,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(AppTab.TODAY) }
                )
                BottomTabToggleItem(
                    label = "My goals",
                    icon = LucideIcons.ListChecks,
                    selected = selected == AppTab.GOALS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(AppTab.GOALS) }
                )
            }
        }
    }
}

@Composable
private fun BottomTabToggleItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(170),
        label = "tab_label_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "tab_label_scale"
    )
    Row(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(25.dp))
            .clickable(onClick = onClick)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
private fun TodayScreen(
    modifier: Modifier,
    focused: Habit?,
    ignitionTargetId: String?,
    onToggleToday: (String) -> Unit,
    onHistory: () -> Unit,
    onOpenTodos: () -> Unit,
    onCreate: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 0.dp)
    ) {
        AnimatedContent(
            targetState = focused,
            modifier = Modifier
                .fillMaxSize(),
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f)) togetherWith (fadeOut(tween(160)) + scaleOut(targetScale = 0.97f)) },
            contentAlignment = Alignment.Center,
            label = "focused_goal"
        ) { habit ->
            if (habit == null) {
                EmptyToday(onCreate)
            } else {
                FocusCard(
                    habit = habit,
                    isIgniting = ignitionTargetId == habit.id,
                    checkInLocked = ignitionTargetId != null,
                    onToggleToday = onToggleToday,
                    onHistory = onHistory,
                    onOpenTodos = onOpenTodos
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(82.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                focused?.let { habit ->
                    Text(
                        habit.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (habit.motivationNote.isNotBlank()) {
                        Text(
                            habit.motivationNote,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                IconButton(onClick = onSettings, modifier = Modifier.size(46.dp)) {
                    Icon(
                        imageVector = LucideIcons.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusCard(
    habit: Habit,
    isIgniting: Boolean,
    checkInLocked: Boolean,
    onToggleToday: (String) -> Unit,
    onHistory: () -> Unit,
    onOpenTodos: () -> Unit
) {
    val today = StreakCalculator.todayString()
    val completed = today in habit.completionHistory
    val flameState = when {
        isIgniting -> FlameState.IGNITING
        completed -> FlameState.COMPLETED
        // Until today's check-in, keep the flame in the same muted state as day zero.
        // The streak count remains visible, but the flame only becomes vivid after completion.
        habit.streakCount > 0 -> FlameState.IDLE
        else -> FlameState.IDLE
    }
    val hasTodos = habit.goalType == GoalType.CHECKLIST && habit.todoItems.isNotEmpty()
    val doneTodos = habit.todoItems.count { it.isCompletedOn(today) }
    val buttonContainer = if (completed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
    val buttonContent = if (completed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary

    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomFlame(
                state = flameState,
                modifier = Modifier.size(208.dp),
                flameColor = habit.accent(),
                ignitionEventKey = habit.id
            )
            Spacer(Modifier.height(2.dp))
            AnimatedContent(
                targetState = habit.streakCount,
                transitionSpec = { (fadeIn(tween(180)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(120)) },
                label = "streak_count"
            ) { count ->
                Text(
                    count.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 78.sp,
                    lineHeight = 72.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                if (habit.streakCount == 1) "day streak" else "days streak",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (hasTodos) {
                Spacer(Modifier.height(24.dp))
                TodoShortcut(doneTodos, habit.todoItems.size, onOpenTodos)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(isIgniting) {
                Text(
                    "Lighting your streak",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            TextButton(onClick = onHistory) {
                Text("View progress", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onToggleToday(habit.id) },
                enabled = !checkInLocked,
                colors = ButtonDefaults.buttonColors(containerColor = buttonContainer, contentColor = buttonContent),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier.widthIn(min = 186.dp, max = 228.dp).height(56.dp)
            ) {
                Text(if (completed) "Undo check-in" else "Check in", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TodoShortcut(completed: Int, total: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(21.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(LucideIcons.ListChecks, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text("$completed of $total tasks", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Icon(LucideIcons.ChevronRight, contentDescription = "Open daily tasks", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun WeekCard(habit: Habit) {
    val today = StreakCalculator.todayString()
    val days = StreakCalculator.weekEndingOn(today)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("THIS WEEK", fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    DayMark(day, day in habit.completionHistory, day == today, day in habit.debts)
                }
            }
        }
    }
}

@Composable
private fun DayMark(day: String, complete: Boolean, isToday: Boolean, protected: Boolean) {
    val title = day.takeLast(2)
    val fill = when {
        complete -> MaterialTheme.colorScheme.primary
        protected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surface
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayName(day), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(fill)
                .border(if (isToday) 2.dp else 1.dp, if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(title, color = if (complete || protected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MomentumCard(habit: Habit) {
    val best = StreakCalculator.longestRun(habit.completionHistory, habit.debts)
    val total = habit.completionHistory.distinct().size
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        Row(Modifier.padding(18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Stat("Best run", best.toString())
            Stat("Check-ins", total.toString())
            Stat("Started", habit.startDate.takeLast(5).replace('-', '/'))
        }
    }
}

@Composable
private fun HistoryDialog(habit: Habit, onDismiss: () -> Unit, onHaptic: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Your progress", fontWeight = FontWeight.ExtraBold) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WeekCard(habit)
            MomentumCard(habit)
            Text("Each completed day keeps the chain moving. A missed day simply starts the next one.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    },
    confirmButton = {
        TextButton(onClick = { onHaptic(); onDismiss() }) { Text("Close") }
    }
)

@Composable
private fun Stat(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun EmptyToday(onCreate: () -> Unit) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomFlame(FlameState.IDLE, modifier = Modifier.size(110.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "Start with one promise.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A tiny action, repeated daily, is enough to create momentum.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onCreate, shape = RoundedCornerShape(18.dp)) {
            Icon(LucideIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create your first goal", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun GoalsScreen(
    modifier: Modifier,
    active: List<Habit>,
    archived: List<Habit>,
    onCreate: () -> Unit,
    onSelect: (Habit) -> Unit,
    onManage: (Habit) -> Unit
) {
    val today = StreakCalculator.todayString()
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MY GOALS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.4.sp)
                    Text("Keep the right promises.", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, maxLines = 2)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onCreate,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(LucideIcons.Plus, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New goal", maxLines = 1, softWrap = false, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        if (active.isEmpty()) {
            item { Text("No active goals yet. Create one to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 28.dp)) }
        }
        items(active, key = Habit::id) { habit ->
            GoalRow(
                habit = habit,
                presentation = goalRowPresentation(habit, today),
                onClick = { onSelect(habit) },
                onAction = { onManage(habit) },
                modifier = Modifier.animateItem()
            )
        }
        if (archived.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                Text("ARCHIVED", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold)
            }
            items(archived, key = Habit::id) { habit ->
                GoalRow(
                    habit = habit,
                    presentation = goalRowPresentation(habit, today),
                    onClick = { onManage(habit) },
                    onAction = { onManage(habit) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun GoalRow(
    habit: Habit,
    presentation: GoalRowPresentation,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "goal_row_press"
    )
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .semantics {
                contentDescription = "${habit.name}, ${presentation.statusText}, ${presentation.streakText}"
            }
            .clickable(
                interactionSource = interactionSource,
                role = Role.Button,
                onClickLabel = if (presentation.primaryAction == GoalRowPrimaryAction.MANAGE) "Manage archived goal" else "Open goal",
                onClick = onClick
            )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CustomFlame(
                state = if (presentation.isCompleted) FlameState.COMPLETED else FlameState.ACTIVE,
                modifier = Modifier.size(42.dp),
                flameColor = habit.accent(),
                ignitionEventKey = habit.id
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    presentation.statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    presentation.streakText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onAction, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = LucideIcons.Ellipsis,
                        contentDescription = "Manage ${habit.name}",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GoalEditorDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onHaptic: () -> Unit,
    onSave: (String, String, String, Boolean, String?, GoalType, List<TodoItem>) -> Unit
) {
    var name by remember(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var note by remember(habit?.id) { mutableStateOf(habit?.motivationNote.orEmpty()) }
    var color by remember(habit?.id) { mutableStateOf(habit?.accent() ?: GoalPalette.options.first().color) }
    var goalType by remember(habit?.id) { mutableStateOf(habit?.goalType ?: GoalType.GOAL) }
    val todoItems = remember(habit?.id) {
        mutableStateListOf<TodoItem>().apply { addAll(habit?.todoItems.orEmpty()) }
    }
    var todoDraft by remember(habit?.id) { mutableStateOf("") }
    var reminder by remember(habit?.id) { mutableStateOf(habit?.reminderEnabled ?: false) }
    var time by remember(habit?.id) { mutableStateOf(habit?.reminderTime ?: "20:00") }
    val isTimeValid = time.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d"))
    val hasTodoItems = todoItems.any { it.title.trim().isNotEmpty() }
    val canSave = name.trim().isNotEmpty() && (!reminder || isTimeValid) && (goalType != GoalType.CHECKLIST || hasTodoItems)
    val addTodo = {
        val cleanTitle = todoDraft.trim()
        if (cleanTitle.isNotEmpty()) {
            todoItems.add(TodoItem(title = cleanTitle))
            todoDraft = ""
            onHaptic()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "New daily goal" else "Edit goal", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (habit == null) "Choose something realistic enough to do today." else "Make this goal fit the life you have now.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedTextField(name, { name = it }, label = { Text("Goal") }, placeholder = { Text("Workout") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Why it matters (optional)") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth())

                Text("FORMAT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalTypeChoice(
                        modifier = Modifier.weight(1f),
                        type = GoalType.GOAL,
                        icon = LucideIcons.Check,
                        title = "Goal",
                        subtitle = "One promise",
                        selected = goalType == GoalType.GOAL,
                        onClick = { goalType = GoalType.GOAL; onHaptic() }
                    )
                    GoalTypeChoice(
                        modifier = Modifier.weight(1f),
                        type = GoalType.CHECKLIST,
                        icon = LucideIcons.ListChecks,
                        title = "To-do list",
                        subtitle = "Daily steps",
                        selected = goalType == GoalType.CHECKLIST,
                        onClick = { goalType = GoalType.CHECKLIST; onHaptic() }
                    )
                }

                AnimatedVisibility(goalType == GoalType.CHECKLIST) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DAILY TASKS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text("These reset automatically tomorrow.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        todoItems.forEachIndexed { index, item ->
                            TodoEditorRow(
                                item = item,
                                onTitleChange = { title -> todoItems[index] = item.copy(title = title) },
                                onRemove = { todoItems.removeAt(index); onHaptic() }
                            )
                        }
                        OutlinedTextField(
                            value = todoDraft,
                            onValueChange = { todoDraft = it },
                            label = { Text("Add a task") },
                            placeholder = { Text("3 sets of squats") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = addTodo) {
                                    Icon(LucideIcons.Plus, contentDescription = "Add task")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("FLAME COLOR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Text("This changes your flame only.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                val selectedColorIndex = GoalPalette.options.indexOfFirst { it.color == color }.coerceAtLeast(0)
                val paletteState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (selectedColorIndex - 1).coerceAtLeast(0)
                )
                LazyRow(
                    state = paletteState,
                    flingBehavior = rememberSnapFlingBehavior(paletteState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GoalPalette.options, key = GoalColorOption::hex) { option ->
                        FlameColorChoice(option, option.color == color) {
                            color = option.color
                            onHaptic()
                        }
                    }
                }

                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminder", fontWeight = FontWeight.Bold)
                        Text("A gentle nudge at your chosen time", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(checked = reminder, onCheckedChange = { reminder = it; onHaptic() })
                }
                AnimatedVisibility(reminder) {
                    Column {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Reminder time") },
                            placeholder = { Text("20:00") },
                            isError = !isTimeValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isTimeValid) {
                            Text("Use 24-hour time, for example 20:00", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, note, color.hex(), reminder, time, goalType, todoItems.toList()) },
                enabled = canSave
            ) {
                Text(if (habit == null) "Create goal" else "Save changes")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun GoalTypeChoice(
    modifier: Modifier,
    type: GoalType,
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
        label = "goal_type_background"
    )
    val foreground by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        label = "goal_type_foreground"
    )
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "goal_type_border")
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(borderWidth, if (selected) background else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = "$title format", tint = foreground, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = foreground, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(subtitle, color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun TodoEditorRow(item: TodoItem, onTitleChange: (String) -> Unit, onRemove: () -> Unit) {
    OutlinedTextField(
        value = item.title,
        onValueChange = onTitleChange,
        leadingIcon = { Icon(LucideIcons.ListChecks, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            IconButton(onClick = onRemove) {
                Icon(LucideIcons.X, contentDescription = "Remove task", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FlameColorChoice(color: GoalColorOption, selected: Boolean, onClick: () -> Unit) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMedium),
        label = "flame_choice_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "flame_choice_scale"
    )
    Box(
        Modifier
            .size(60.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(
                borderWidth,
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .semantics { contentDescription = "${color.name} flame color" }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CustomFlame(FlameState.ACTIVE, modifier = Modifier.size(42.dp), flameColor = color.color)
    }
}

@Composable
private fun GoalManageDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onHaptic: () -> Unit
) {
    var confirmDelete by remember(habit.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.name, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (habit.isArchived) {
                    Text("This goal is archived. Restore it before editing or tracking it again.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    ManageAction(LucideIcons.RotateCcw, "Restore goal", "Return it to your active goals") { onHaptic(); onRestore() }
                } else {
                    Text("Manage this daily goal from one place.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    ManageAction(LucideIcons.Palette, "Edit details", "Change the goal, flame, or tasks") { onHaptic(); onEdit() }
                    ManageAction(LucideIcons.Archive, "Archive goal", "Hide it without deleting history") { onHaptic(); onArchive() }
                    ManageAction(LucideIcons.Trash2, "Delete permanently", "Remove this goal and its history", destructive = true) {
                        onHaptic()
                        confirmDelete = true
                    }
                }
                AnimatedVisibility(confirmDelete) {
                    Text("Tap Delete below to permanently remove this goal.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            if (confirmDelete) {
                Button(onClick = { onHaptic(); onDelete() }) { Text("Delete") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ManageAction(icon: ImageVector, title: String, detail: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(LucideIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun TodoDialog(habit: Habit, onDismiss: () -> Unit, onToggle: (String) -> Unit, onHaptic: () -> Unit) {
    val today = StreakCalculator.todayString()
    val completed = habit.todoItems.count { it.isCompletedOn(today) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LucideIcons.ListChecks, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Text("Today's tasks", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$completed of ${habit.todoItems.size} complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                habit.todoItems.forEach { item ->
                    TodoTaskRow(
                        item = item,
                        completed = item.isCompletedOn(today),
                        onClick = {
                            onHaptic()
                            onToggle(item.id)
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onHaptic(); onDismiss() }) { Text("Done") } }
    )
}

@Composable
private fun TodoTaskRow(item: TodoItem, completed: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (completed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "todo_background"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(23.dp)
                .clip(CircleShape)
                .background(if (completed) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completed) Icon(LucideIcons.Check, contentDescription = "Completed", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(11.dp))
        Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsDialog(
    mode: String,
    haptics: Boolean,
    onDismiss: () -> Unit,
    onTheme: (String) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onHaptic: () -> Unit
) {
    var showReset by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionTitle(LucideIcons.Palette, "Appearance")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeModeChoice(Modifier.weight(1f), "SYSTEM", "System", LucideIcons.Monitor, mode == "SYSTEM") { onHaptic(); onTheme("SYSTEM") }
                    ThemeModeChoice(Modifier.weight(1f), "LIGHT", "Light", LucideIcons.Sun, mode == "LIGHT") { onHaptic(); onTheme("LIGHT") }
                    ThemeModeChoice(Modifier.weight(1f), "DARK", "Dark", LucideIcons.Moon, mode == "DARK") { onHaptic(); onTheme("DARK") }
                }
                SettingsToggleRow(
                    icon = LucideIcons.Smartphone,
                    title = "Haptic feedback",
                    detail = "A tactile response for key actions",
                    checked = haptics,
                    onCheckedChange = { onHaptic(); onHaptics(it) }
                )
                HorizontalDivider()
                SettingsSectionTitle(LucideIcons.Download, "Your data")
                SettingsActionRow(LucideIcons.Download, "Export backup JSON", "Save a portable backup file", onClick = { onHaptic(); onExport() })
                SettingsActionRow(LucideIcons.Upload, "Import backup JSON", "Restore from a backup file", onClick = { onHaptic(); onImport() })
                SettingsActionRow(LucideIcons.RotateCcw, "Reset local data", "Remove every local goal and check-in", destructive = true, onClick = { onHaptic(); showReset = true })
                AnimatedVisibility(showReset) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("This cannot be undone once confirmed.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Button(onClick = { onHaptic(); onReset() }, modifier = Modifier.fillMaxWidth()) { Text("Confirm reset") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onHaptic(); onDismiss() }) { Text("Done") } }
    )
}

@Composable
private fun SettingsSectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(title.uppercase(Locale.US), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun ThemeModeChoice(modifier: Modifier, key: String, label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant, label = "theme_mode_background")
    val foreground by animateColorAsState(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface, label = "theme_mode_foreground")
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(icon: ImageVector, title: String, detail: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(LucideIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
    }
}

private fun Habit.accent(): Color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(GoalPalette.options.first().color)
private fun Color.hex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
private fun dayName(value: String): String = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.let { SimpleDateFormat("EE", Locale.getDefault()).format(it) } ?: ""
}.getOrDefault("")
