package com.hkm.streakwidget.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.streakwidget.R
import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.StreakNote
import com.hkm.streakwidget.model.TodoItem
import com.hkm.streakwidget.model.VisualTheme
import com.hkm.streakwidget.ui.AppScreen
import com.hkm.streakwidget.ui.StreakViewModel
import com.hkm.streakwidget.ui.components.AppPredictiveBackHandler
import com.hkm.streakwidget.ui.components.CustomBonsai
import com.hkm.streakwidget.ui.components.CustomFlame
import com.hkm.streakwidget.ui.components.FlameState
import com.hkm.streakwidget.ui.components.MilestoneOverlay
import com.hkm.streakwidget.ui.components.TemplatePickerDialog
import com.hkm.streakwidget.ui.components.appPredictiveBackVisual
import com.hkm.streakwidget.ui.insights.InsightsScreen
import com.hkm.streakwidget.ui.settings.SettingsScreen
import com.hkm.streakwidget.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var backProgress by remember { mutableStateOf(0f) }
    var editorTarget by remember { mutableStateOf<Habit?>(null) }
    var managingHabit by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showTodos by remember { mutableStateOf(false) }
    var showClaimDebtDialog by remember { mutableStateOf(false) }
    var showDebtManageDialog by remember { mutableStateOf(false) }
    var ignitionTargetId by remember { mutableStateOf<String?>(null) }
    val reduceMotion = viewModel.reduceMotion.value
    val pendingScreen by viewModel.pendingScreen.collectAsState()
    val milestoneEvent by viewModel.milestone.collectAsState()

    LaunchedEffect(pendingScreen) {
        pendingScreen?.let { requested ->
            screen = requested
            viewModel.consumeRequestedScreen()
        }
    }

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
    val togglePause: (Boolean) -> Unit = { wasPaused ->
        viewModel.togglePauseToday()
        confirmHaptic()
        scope.launch {
            snackbar.showSnackbar(
                context.getString(if (wasPaused) R.string.snack_resumed else R.string.snack_paused)
            )
        }
    }

    AppPredictiveBackHandler(
        enabled = screen != AppScreen.HOME || tab == AppTab.GOALS,
        onBack = {
            if (screen != AppScreen.HOME) {
                screen = AppScreen.HOME
            } else {
                tab = AppTab.TODAY
            }
            tapHaptic()
        },
        onProgress = { backProgress = it }
    )

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(viewModel.exportBackup())
                } ?: error("Could not create the backup file")
            }.isSuccess
            snackbar.showSnackbar(
                context.getString(if (saved) R.string.snack_backup_saved else R.string.snack_backup_failed)
            )
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val restored = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?.let(viewModel::importBackup) == true
            }.getOrDefault(false)
            snackbar.showSnackbar(
                context.getString(if (restored) R.string.snack_backup_restored else R.string.snack_backup_invalid)
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                visible = screen == AppScreen.HOME,
                enter = fadeIn(tween(200)) +
                    slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 3 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it / 3 }
            ) {
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
        }
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = screen,
            modifier = Modifier
                .padding(scaffoldPadding)
                .appPredictiveBackVisual(backProgress),
            transitionSpec = {
                when {
                    reduceMotion -> fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                    targetState == AppScreen.HOME ->
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 10 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(200)))
                    else ->
                        (slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(230))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it / 10 } + fadeOut(tween(160)))
                }
            },
            label = "app_screen"
        ) { currentScreen ->
            when (currentScreen) {
                AppScreen.HOME -> AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        when {
                            reduceMotion -> fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                            targetState.ordinal > initialState.ordinal ->
                                (slideInHorizontally(tween(430, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(250)) { -it / 8 } + fadeOut(tween(180)))
                            else ->
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
                                    reduceMotion -> {
                                        viewModel.toggleToday(habitId)
                                        confirmHaptic()
                                    }
                                    else -> scope.launch {
                                        tapHaptic()
                                        ignitionTargetId = habitId
                                        delay(1050)
                                        viewModel.toggleToday(habitId)
                                        confirmHaptic()
                                        delay(2550)
                                        if (ignitionTargetId == habitId) ignitionTargetId = null
                                    }
                                }
                            },
                            onTogglePause = togglePause,
                            onCreate = {
                                editorTarget = null
                                showEditor = true
                                tapHaptic()
                            },
                            onSettings = {
                                screen = AppScreen.SETTINGS
                                tapHaptic()
                            },
                            onRescue = {
                                tapHaptic()
                                focused?.let { target ->
                                    if (target.unclaimedMissedDates(StreakCalculator.todayString()).isNotEmpty()) {
                                        showClaimDebtDialog = true
                                    } else {
                                        showDebtManageDialog = true
                                    }
                                }
                            },
                            onOpenTodos = {
                                showTodos = true
                                tapHaptic()
                            },
                            onNotes = {
                                showNotes = true
                                tapHaptic()
                            },
                            onInsights = {
                                screen = AppScreen.INSIGHTS
                                tapHaptic()
                            },
                            onHaptic = tapHaptic
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

                AppScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.HOME },
                    onExport = { exportBackup.launch("streak-widget-backup.json") },
                    onImport = { importBackup.launch(arrayOf("application/json", "text/plain")) },
                    onReset = {
                        viewModel.resetAllData()
                        screen = AppScreen.HOME
                        scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_all_reset)) }
                    },
                    onHaptic = tapHaptic
                )

                AppScreen.INSIGHTS -> InsightsScreen(
                    habit = focused,
                    reduceMotion = reduceMotion,
                    onBack = { screen = AppScreen.HOME },
                    onHaptic = tapHaptic
                )
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            habit = editorTarget,
            onDismiss = { showEditor = false },
            onHaptic = tapHaptic,
            onSave = { name, note, color, reminder, time, type, todoItems, theme ->
                if (editorTarget == null) {
                    viewModel.createHabit(name, note, color, reminder, time, type, todoItems, theme)
                } else {
                    viewModel.updateFocusedHabit(name, note, color, reminder, time, type, todoItems, theme)
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
                scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_goal_archived)) }
            },
            onRestore = {
                viewModel.restoreHabit(habit)
                managingHabit = null
                scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_goal_restored)) }
            },
            onDelete = {
                viewModel.deleteFocusedHabit()
                managingHabit = null
                scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_goal_deleted)) }
            },
            onHaptic = confirmHaptic
        )
    }

    focused?.let { habit ->
        if (showClaimDebtDialog) {
            val unclaimed = habit.unclaimedMissedDates()
            if (unclaimed.isNotEmpty()) {
                ClaimDebtDialog(
                    unclaimedDates = unclaimed,
                    onDismiss = { showClaimDebtDialog = false },
                    onConfirm = { dates ->
                        viewModel.claimDebt(habit.id, dates)
                        confirmHaptic()
                        scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_streak_protected)) }
                    },
                    onHaptic = tapHaptic
                )
            } else {
                showClaimDebtDialog = false
            }
        }
        if (showDebtManageDialog) {
            DebtManagementDialog(
                habit = habit,
                onDismiss = { showDebtManageDialog = false },
                onPayDebt = { date ->
                    viewModel.payDebt(habit.id, date)
                    confirmHaptic()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.snack_day_paid, date)) }
                },
                onHaptic = tapHaptic
            )
        }
        if (showNotes) {
            NotesDialog(
                habit = habit,
                onDismiss = { showNotes = false },
                onAdd = { viewModel.addNote(it); confirmHaptic() },
                onDelete = { viewModel.deleteNote(it); tapHaptic() },
                onHaptic = tapHaptic
            )
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

    milestoneEvent?.let { event ->
        MilestoneOverlay(
            event = event,
            reduceMotion = reduceMotion,
            onDismiss = viewModel::consumeMilestone,
            onHaptic = confirmHaptic
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
                    label = stringResource(R.string.tab_today),
                    icon = AppIcons.Check,
                    selected = selected == AppTab.TODAY,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(AppTab.TODAY) }
                )
                BottomTabToggleItem(
                    label = stringResource(R.string.tab_goals),
                    icon = AppIcons.ListChecks,
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
    onTogglePause: (Boolean) -> Unit,
    onCreate: () -> Unit,
    onSettings: () -> Unit,
    onRescue: () -> Unit,
    onOpenTodos: () -> Unit,
    onNotes: () -> Unit,
    onInsights: () -> Unit,
    onHaptic: () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 0.dp)
    ) {
        AnimatedContent(
            targetState = focused,
            contentKey = { it?.id },
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
                    onTogglePause = onTogglePause,
                    onRescue = onRescue,
                    onOpenTodos = onOpenTodos,
                    onNotes = onNotes,
                    onInsights = onInsights,
                    onHaptic = onHaptic
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
            IconButton(onClick = onSettings, modifier = Modifier.size(46.dp)) {
                Icon(
                    imageVector = AppIcons.Gear,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp)
                )
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
    onTogglePause: (Boolean) -> Unit,
    onRescue: () -> Unit,
    onOpenTodos: () -> Unit,
    onNotes: () -> Unit,
    onInsights: () -> Unit,
    onHaptic: () -> Unit
) {
    val today = StreakCalculator.todayString()
    val completed = today in habit.completionHistory
    val paused = habit.isPausedOn(today)
    var expanded by remember(habit.id) { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    val choose: (() -> Unit) -> Unit = { action ->
        expanded = false
        action()
    }
    val flameState = when {
        isIgniting -> FlameState.IGNITING
        completed -> FlameState.COMPLETED
        // Until today's check-in, keep the flame in the same muted state as day zero.
        // The streak count remains visible, but the flame only becomes vivid after completion.
        habit.streakCount > 0 -> FlameState.IDLE
        else -> FlameState.IDLE
    }
    val buttonContainer = when {
        paused -> MaterialTheme.colorScheme.tertiaryContainer
        completed -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val buttonContent = when {
        paused -> MaterialTheme.colorScheme.onTertiaryContainer
        completed -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (habit.theme == VisualTheme.TREE) {
                    CustomBonsai(
                        streakCount = habit.streakCount,
                        state = flameState,
                        modifier = Modifier.size(220.dp),
                        accentColor = habit.accent(),
                        eventKey = habit.id
                    )
                } else {
                    CustomFlame(
                        state = flameState,
                        modifier = Modifier.size(220.dp),
                        flameColor = habit.accent(),
                        ignitionEventKey = habit.id
                    )
                }
            }
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
                pluralStringResource(R.plurals.today_streak_label, habit.streakCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (paused) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcons.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.today_paused_title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isIgniting,
                enter = fadeIn(tween(180)) + expandVertically(tween(220), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160), shrinkTowards = Alignment.Bottom)
            ) {
                Text(
                    stringResource(
                        if (habit.theme == VisualTheme.TREE) R.string.today_nurturing else R.string.today_lighting
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(170)) + expandVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Bottom
                ),
                exit = fadeOut(tween(110)) + shrinkVertically(tween(150), shrinkTowards = Alignment.Bottom)
            ) {
                FocusActions(
                    habit = habit,
                    paused = paused,
                    onTogglePause = { choose { onTogglePause(paused) } },
                    onRescue = { choose(onRescue) },
                    onOpenTodos = { choose(onOpenTodos) },
                    onNotes = { choose(onNotes) },
                    onInsights = { choose(onInsights) }
                )
            }
            IconButton(
                onClick = {
                    expanded = !expanded
                    if (expanded) onHaptic()
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = if (expanded) AppIcons.X else AppIcons.Sliders,
                    contentDescription = stringResource(
                        if (expanded) R.string.cd_close_menu else R.string.cd_more_options
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    expanded = false
                    if (paused) onTogglePause(true) else onToggleToday(habit.id)
                },
                enabled = paused || !checkInLocked,
                colors = ButtonDefaults.buttonColors(containerColor = buttonContainer, contentColor = buttonContent),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier.widthIn(min = 186.dp, max = 228.dp).height(56.dp)
            ) {
                val label = when {
                    paused -> stringResource(R.string.today_resume_today)
                    completed -> stringResource(R.string.today_undo_check_in)
                    else -> stringResource(R.string.today_check_in)
                }
                Text(label, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FocusActions(
    habit: Habit,
    paused: Boolean,
    onTogglePause: () -> Unit,
    onRescue: () -> Unit,
    onOpenTodos: () -> Unit,
    onNotes: () -> Unit,
    onInsights: () -> Unit
) {
    val today = StreakCalculator.todayString()
    val hasTodos = habit.goalType == GoalType.CHECKLIST && habit.todoItems.isNotEmpty()
    val doneTodos = habit.todoItems.count { it.isCompletedOn(today) }
    val unclaimed = habit.unclaimedMissedDates(today).size
    val rescueCount = unclaimed + habit.debts.size

    Column(
        Modifier
            .widthIn(max = 236.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(6.dp)
    ) {
        FocusActionRow(
            icon = if (paused) AppIcons.Play else AppIcons.Pause,
            label = stringResource(if (paused) R.string.today_resume_today else R.string.today_pause_today),
            status = if (paused) stringResource(R.string.today_paused_title) else null,
            onClick = onTogglePause
        )
        if (rescueCount > 0) {
            FocusActionRow(
                icon = AppIcons.Shield,
                label = stringResource(R.string.debt_manage_title),
                status = pluralStringResource(R.plurals.sheet_rescue_days, rescueCount, rescueCount),
                accent = unclaimed > 0,
                onClick = onRescue
            )
        }
        if (hasTodos) {
            FocusActionRow(
                icon = AppIcons.ListChecks,
                label = stringResource(R.string.sheet_tasks),
                status = stringResource(R.string.sheet_tasks_progress, doneTodos, habit.todoItems.size),
                onClick = onOpenTodos
            )
        }
        FocusActionRow(
            icon = AppIcons.Note,
            label = stringResource(R.string.notes_title),
            status = if (habit.notes.isEmpty()) {
                stringResource(R.string.sheet_notes_empty)
            } else {
                pluralStringResource(R.plurals.sheet_notes_count, habit.notes.size, habit.notes.size)
            },
            onClick = onNotes
        )
        FocusActionRow(
            icon = AppIcons.Chart,
            label = stringResource(R.string.insights_title),
            chevron = true,
            onClick = onInsights
        )
    }
}

@Composable
private fun FocusActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    status: String? = null,
    accent: Boolean = false,
    chevron: Boolean = false
) {
    val tint = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (status != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                status,
                color = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        if (chevron) {
            Spacer(Modifier.width(2.dp))
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun ClaimDebtDialog(
    unclaimedDates: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    onHaptic: () -> Unit
) {
    var committed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.debt_claim_title), fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.debt_claim_body,
                        unclaimedDates.size,
                        unclaimedDates.joinToString(", ")
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.debt_claim_detail),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { committed = !committed }
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = committed,
                            onCheckedChange = { committed = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.debt_claim_commit),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onHaptic()
                    onConfirm(unclaimedDates)
                    onDismiss()
                },
                enabled = committed
            ) {
                Text(stringResource(R.string.debt_claim_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.debt_claim_later))
            }
        }
    )
}

@Composable
private fun DebtManagementDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onPayDebt: (String) -> Unit,
    onHaptic: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.Flame,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.debt_manage_title), fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.debt_manage_body, habit.debts.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Column(
                    Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habit.debts.forEach { debtDate ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(debtDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        stringResource(R.string.debt_unpaid),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = {
                                        onHaptic()
                                        onPayDebt(debtDate)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(stringResource(R.string.debt_pay), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

/**
 * One-shot 1.8s burst that mirrors the flame ignition ritual: an early white
 * flash, then twin shockwave rings expanding outward behind the flame.
 */
@Composable
private fun IgnitionBurstOverlay(visible: Boolean, flameColor: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 1_800, easing = FastOutSlowInEasing))
        }
    }
    // Never draw outside the ignition ritual: at rest even a faint ring reads as a bug.
    if (!visible) return
    val p = progress.value
    val ring2 = ((p - 0.14f) / 0.86f).coerceIn(0f, 1f)
    val flash = (1f - p * 3.2f).coerceIn(0f, 1f)
    Canvas(Modifier.size(272.dp)) {
        val minSide = size.minDimension
        if (flash > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f * flash),
                        flameColor.copy(alpha = 0.28f * flash),
                        Color.Transparent
                    ),
                    radius = minSide * 0.42f
                )
            )
        }
        drawCircle(
            color = flameColor.copy(alpha = (1f - p) * 0.5f),
            radius = minSide * (0.3f + 0.34f * p),
            style = Stroke(width = minSide * 0.012f)
        )
        drawCircle(
            color = Color.White.copy(alpha = (1f - ring2) * 0.3f),
            radius = minSide * (0.26f + 0.3f * ring2),
            style = Stroke(width = minSide * 0.006f)
        )
    }
}

@Composable
private fun NotesDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onHaptic: () -> Unit
) {
    var draft by remember(habit.id) { mutableStateOf("") }
    val notes = habit.notes.sortedWith(
        compareByDescending<StreakNote> { it.date }.thenByDescending { it.createdAt }
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Note, contentDescription = null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.notes_title), fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notes.isEmpty()) {
                    Text(
                        stringResource(R.string.notes_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    notes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                        ) {
                            Row(
                                Modifier.padding(start = 13.dp, top = 10.dp, end = 5.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(note.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(note.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { onHaptic(); onDelete(note.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        AppIcons.Trash,
                                        contentDescription = stringResource(R.string.cd_delete_note),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(draft)
                    draft = ""
                },
                enabled = draft.trim().isNotEmpty()
            ) { Text(stringResource(R.string.notes_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } }
    )
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
            stringResource(R.string.today_empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.today_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onCreate, shape = RoundedCornerShape(18.dp)) {
            Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.today_create_first), fontWeight = FontWeight.ExtraBold)
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
    val copy = GoalRowCopy(
        archived = stringResource(R.string.goal_status_archived),
        allCompleted = stringResource(R.string.goal_all_completed),
        progress = stringResource(R.string.goal_progress),
        completedToday = stringResource(R.string.goal_completed_today),
        readyToday = stringResource(R.string.goal_ready_today),
        pausedToday = stringResource(R.string.goal_paused_today),
        day = stringResource(R.string.goal_day),
        days = stringResource(R.string.goal_days)
    )
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.goals_eyebrow),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        stringResource(R.string.goals_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 25.sp,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onCreate,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.goals_new),
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        if (active.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.goals_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 28.dp)
                )
            }
        }
        items(active, key = Habit::id) { habit ->
            GoalRow(
                habit = habit,
                presentation = goalRowPresentation(habit, today, copy),
                onClick = { onSelect(habit) },
                onAction = { onManage(habit) },
                modifier = Modifier.animateItem()
            )
        }
        if (archived.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.goals_archived),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            items(archived, key = Habit::id) { habit ->
                GoalRow(
                    habit = habit,
                    presentation = goalRowPresentation(habit, today, copy),
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
                onClickLabel = if (presentation.primaryAction == GoalRowPrimaryAction.MANAGE) {
                    stringResource(R.string.cd_manage_goal, habit.name)
                } else {
                    stringResource(R.string.cd_goal, habit.name)
                },
                onClick = onClick
            )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (habit.theme == VisualTheme.TREE) {
                CustomBonsai(
                    streakCount = habit.streakCount,
                    state = if (presentation.isCompleted) FlameState.COMPLETED else FlameState.ACTIVE,
                    modifier = Modifier.size(42.dp),
                    accentColor = habit.accent(),
                    eventKey = habit.id
                )
            } else {
                CustomFlame(
                    state = if (presentation.isCompleted) FlameState.COMPLETED else FlameState.ACTIVE,
                    modifier = Modifier.size(42.dp),
                    flameColor = habit.accent(),
                    ignitionEventKey = habit.id
                )
            }
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
                        imageVector = AppIcons.More,
                        contentDescription = stringResource(R.string.cd_manage_goal, habit.name),
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
    onSave: (String, String, String, Boolean, String?, GoalType, List<TodoItem>, VisualTheme) -> Unit
) {
    var name by remember(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var note by remember(habit?.id) { mutableStateOf(habit?.motivationNote.orEmpty()) }
    var color by remember(habit?.id) { mutableStateOf(habit?.accent() ?: GoalPalette.options.first().color) }
    var theme by remember(habit?.id) { mutableStateOf(habit?.theme ?: VisualTheme.FLAME) }
    var goalType by remember(habit?.id) { mutableStateOf(habit?.goalType ?: GoalType.GOAL) }
    val todoItems = remember(habit?.id) {
        mutableStateListOf<TodoItem>().apply { addAll(habit?.todoItems.orEmpty()) }
    }
    var todoDraft by remember(habit?.id) { mutableStateOf("") }
    var reminder by remember(habit?.id) { mutableStateOf(habit?.reminderEnabled ?: false) }
    var time by remember(habit?.id) { mutableStateOf(habit?.reminderTime ?: "20:00") }
    var showTemplates by remember(habit?.id) { mutableStateOf(false) }
    val templateContext = LocalContext.current
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
        title = {
            Text(
                stringResource(if (habit == null) R.string.editor_new_title else R.string.editor_edit_title),
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(if (habit == null) R.string.editor_new_body else R.string.editor_edit_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (habit == null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onHaptic()
                                showTemplates = true
                            }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                                Icon(
                                    AppIcons.Sparkle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp).size(17.dp)
                                )
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.editor_templates),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    stringResource(R.string.editor_templates_detail),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(stringResource(R.string.editor_name)) },
                    placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(stringResource(R.string.editor_why)) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    stringResource(R.string.editor_theme_section),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChoiceCard(
                        modifier = Modifier.weight(1f),
                        icon = AppIcons.Flame,
                        title = stringResource(R.string.editor_theme_flame),
                        subtitle = stringResource(R.string.editor_theme_flame_detail),
                        selected = theme == VisualTheme.FLAME,
                        onClick = { theme = VisualTheme.FLAME; onHaptic() }
                    )
                    ThemeChoiceCard(
                        modifier = Modifier.weight(1f),
                        icon = AppIcons.Plant,
                        title = stringResource(R.string.editor_theme_tree),
                        subtitle = stringResource(R.string.editor_theme_tree_detail),
                        selected = theme == VisualTheme.TREE,
                        onClick = { theme = VisualTheme.TREE; onHaptic() }
                    )
                }

                Text(
                    stringResource(R.string.editor_format_section),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalTypeChoice(
                        modifier = Modifier.weight(1f),
                        type = GoalType.GOAL,
                        icon = AppIcons.Check,
                        title = stringResource(R.string.editor_format_goal),
                        subtitle = stringResource(R.string.editor_format_goal_detail),
                        selected = goalType == GoalType.GOAL,
                        onClick = { goalType = GoalType.GOAL; onHaptic() }
                    )
                    GoalTypeChoice(
                        modifier = Modifier.weight(1f),
                        type = GoalType.CHECKLIST,
                        icon = AppIcons.ListChecks,
                        title = stringResource(R.string.editor_format_checklist),
                        subtitle = stringResource(R.string.editor_format_checklist_detail),
                        selected = goalType == GoalType.CHECKLIST,
                        onClick = { goalType = GoalType.CHECKLIST; onHaptic() }
                    )
                }

                AnimatedVisibility(goalType == GoalType.CHECKLIST) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.editor_tasks_section),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            stringResource(R.string.editor_tasks_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
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
                            label = { Text(stringResource(R.string.editor_task_label)) },
                            placeholder = { Text(stringResource(R.string.editor_task_placeholder)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = addTodo, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        AppIcons.Plus,
                                        contentDescription = stringResource(R.string.cd_add_task)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    stringResource(
                        if (theme == VisualTheme.TREE) R.string.editor_color_accent else R.string.editor_color_flame
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    stringResource(
                        if (theme == VisualTheme.TREE) {
                            R.string.editor_color_accent_body
                        } else {
                            R.string.editor_color_flame_body
                        }
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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
                        Text(stringResource(R.string.editor_reminder), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.editor_reminder_detail),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Switch(checked = reminder, onCheckedChange = { reminder = it; onHaptic() })
                }
                AnimatedVisibility(reminder) {
                    Column {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text(stringResource(R.string.editor_time)) },
                            placeholder = { Text(stringResource(R.string.editor_time_placeholder)) },
                            isError = !isTimeValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isTimeValid) {
                            Text(
                                stringResource(R.string.editor_time_error),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, note, color.hex(), reminder, time, goalType, todoItems.toList(), theme) },
                enabled = canSave
            ) {
                Text(
                    stringResource(if (habit == null) R.string.editor_create else R.string.editor_save)
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )

    if (showTemplates) {
        TemplatePickerDialog(
            onDismiss = { showTemplates = false },
            onPick = { template ->
                name = templateContext.getString(template.nameRes)
                note = templateContext.getString(template.noteRes)
                color = runCatching {
                    Color(android.graphics.Color.parseColor(template.colorHex))
                }.getOrDefault(GoalPalette.options.first().color)
                theme = template.theme
                goalType = template.goalType
                todoItems.clear()
                template.taskRes.forEach { todoItems.add(TodoItem(title = templateContext.getString(it))) }
                showTemplates = false
                onHaptic()
            },
            onHaptic = onHaptic
        )
    }
}

@Composable
private fun ThemeChoiceCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
        label = "theme_choice_background"
    )
    val foreground by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        label = "theme_choice_foreground"
    )
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "theme_choice_border")
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(borderWidth, if (selected) background else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = stringResource(R.string.cd_theme, title),
            tint = foreground,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(title, color = foreground, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(subtitle, color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
    }
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
        Icon(
            icon,
            contentDescription = stringResource(R.string.cd_format, title),
            tint = foreground,
            modifier = Modifier.size(19.dp)
        )
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
        leadingIcon = { Icon(AppIcons.ListChecks, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(
                    AppIcons.X,
                    contentDescription = stringResource(R.string.cd_remove_task),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
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
    val colorDescription = stringResource(R.string.cd_color, color.name)
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
            .semantics { contentDescription = colorDescription }
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
                    Text(
                        stringResource(R.string.manage_archived_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    ManageAction(
                        AppIcons.Restore,
                        stringResource(R.string.manage_restore),
                        stringResource(R.string.manage_restore_detail)
                    ) { onHaptic(); onRestore() }
                } else {
                    Text(
                        stringResource(R.string.manage_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    ManageAction(
                        AppIcons.Pencil,
                        stringResource(R.string.manage_edit),
                        stringResource(R.string.manage_edit_detail)
                    ) { onHaptic(); onEdit() }
                    ManageAction(
                        AppIcons.Archive,
                        stringResource(R.string.manage_archive),
                        stringResource(R.string.manage_archive_detail)
                    ) { onHaptic(); onArchive() }
                    ManageAction(
                        AppIcons.Trash,
                        stringResource(R.string.manage_delete),
                        stringResource(R.string.manage_delete_detail),
                        destructive = true
                    ) {
                        onHaptic()
                        confirmDelete = true
                    }
                }
                AnimatedVisibility(confirmDelete) {
                    Text(
                        stringResource(R.string.manage_delete_warning),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (confirmDelete) {
                Button(onClick = { onHaptic(); onDelete() }) {
                    Text(stringResource(R.string.manage_delete_button))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } }
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
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
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
                Icon(AppIcons.ListChecks, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.todos_title), fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.todos_progress, completed, habit.todoItems.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
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
        confirmButton = { TextButton(onClick = { onHaptic(); onDismiss() }) { Text(stringResource(R.string.common_done)) } }
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
            if (completed) {
                Icon(
                    AppIcons.Check,
                    contentDescription = stringResource(R.string.todos_completed),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

private fun Habit.accent(): Color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(GoalPalette.options.first().color)
private fun Color.hex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
