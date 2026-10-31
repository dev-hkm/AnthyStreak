package com.hkm.streakwidget.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.hkm.streakwidget.R
import com.hkm.streakwidget.data.AppLanguage
import com.hkm.streakwidget.ui.StreakViewModel
import com.hkm.streakwidget.ui.theme.AppIcons
import java.util.Locale

/** Full-screen preferences that follow the rest of the app's calm, grouped rhythm. */
@Composable
fun SettingsScreen(
    viewModel: StreakViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(notificationsAllowed(context)) }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionAsked = true
        if (granted) onHaptic()
    }
    val lifecycleOwner = remember(context) { context.findActivity() as? LifecycleOwner }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = notificationsAllowed(context)
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.common_settings), onBack = {
            onHaptic()
            onBack()
        })
        Text(
            stringResource(R.string.settings_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(26.dp))

        SectionTitle(AppIcons.Palette, stringResource(R.string.settings_appearance))
        PreferenceGroup {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeChoice(
                        label = stringResource(R.string.settings_theme_system),
                        icon = AppIcons.Monitor,
                        selected = viewModel.themeMode.value == "SYSTEM",
                        modifier = Modifier.weight(1f)
                    ) { onHaptic(); viewModel.updateThemeMode("SYSTEM") }
                    ThemeChoice(
                        label = stringResource(R.string.settings_theme_light),
                        icon = AppIcons.Sun,
                        selected = viewModel.themeMode.value == "LIGHT",
                        modifier = Modifier.weight(1f)
                    ) { onHaptic(); viewModel.updateThemeMode("LIGHT") }
                    ThemeChoice(
                        label = stringResource(R.string.settings_theme_dark),
                        icon = AppIcons.Moon,
                        selected = viewModel.themeMode.value == "DARK",
                        modifier = Modifier.weight(1f)
                    ) { onHaptic(); viewModel.updateThemeMode("DARK") }
                }
            }
            InsetDivider()
            Column(Modifier.padding(vertical = 4.dp)) {
                SettingsRowLabel(stringResource(R.string.settings_language), stringResource(R.string.settings_language_detail))
                AppLanguage.entries.forEach { language ->
                    LanguageRow(
                        label = when (language) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.VIETNAMESE -> "Tiếng Việt"
                        },
                        selected = viewModel.appLanguage.value == language
                    ) {
                        onHaptic()
                        viewModel.updateAppLanguage(language)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        SectionTitle(AppIcons.Phone, stringResource(R.string.settings_feel))
        PreferenceGroup {
            ToggleRow(
                icon = AppIcons.Phone,
                title = stringResource(R.string.settings_haptics),
                detail = stringResource(R.string.settings_haptics_detail),
                checked = viewModel.hapticEnabled.value
            ) { onHaptic(); viewModel.updateHaptics(it) }
            InsetDivider()
            ToggleRow(
                icon = AppIcons.Sparkle,
                title = stringResource(R.string.settings_reduce_motion),
                detail = stringResource(R.string.settings_reduce_motion_detail),
                checked = viewModel.reduceMotion.value
            ) { onHaptic(); viewModel.updateReduceMotion(it) }
        }
        Spacer(Modifier.height(22.dp))

        SectionTitle(AppIcons.Bell, stringResource(R.string.settings_notifications))
        PreferenceGroup {
            ToggleRow(
                icon = AppIcons.Bell,
                title = stringResource(R.string.settings_reminders),
                detail = stringResource(R.string.settings_reminders_detail),
                checked = viewModel.remindersEnabled.value
            ) { onHaptic(); viewModel.updateRemindersEnabled(it) }
            InsetDivider()
            ToggleRow(
                icon = AppIcons.Shield,
                title = stringResource(R.string.settings_rescue),
                detail = stringResource(R.string.settings_rescue_detail),
                checked = viewModel.rescueRemindersEnabled.value
            ) { onHaptic(); viewModel.updateRescueRemindersEnabled(it) }
            InsetDivider()
            PermissionRow(
                granted = permissionGranted,
                onAllow = {
                    onHaptic()
                    if (permissionGranted) {
                        openNotificationSettings(context)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionAsked) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }
            )
        }
        Spacer(Modifier.height(22.dp))

        SectionTitle(AppIcons.Download, stringResource(R.string.settings_data))
        PreferenceGroup {
            ActionRow(
                icon = AppIcons.Download,
                title = stringResource(R.string.settings_export),
                detail = stringResource(R.string.settings_export_detail)
            ) { onHaptic(); onExport() }
            InsetDivider()
            ActionRow(
                icon = AppIcons.Upload,
                title = stringResource(R.string.settings_import),
                detail = stringResource(R.string.settings_import_detail)
            ) { onHaptic(); onImport() }
            InsetDivider()
            ActionRow(
                icon = AppIcons.Warning,
                title = stringResource(R.string.settings_reset),
                detail = stringResource(R.string.settings_reset_detail),
                destructive = true
            ) { onHaptic(); showResetConfirm = !showResetConfirm }
            AnimatedVisibility(
                visible = showResetConfirm,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120))
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.settings_reset_warning),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onHaptic()
                            showResetConfirm = false
                            onReset()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.settings_reset_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        SectionTitle(AppIcons.Info, stringResource(R.string.settings_about))
        PreferenceGroup {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    IconChip(AppIcons.Heart, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.settings_about_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_version, appVersion(context)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                AppIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            title.uppercase(Locale.getDefault()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun PreferenceGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun SettingsRowLabel(title: String, detail: String) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ThemeChoice(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "settings_theme_background"
    )
    val foreground by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "settings_theme_foreground"
    )
    Column(
        modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tint, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
        if (selected) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurface) {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(3.dp).size(13.dp)
                )
            }
        } else {
            Box(Modifier.size(19.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Icon(
            AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun PermissionRow(granted: Boolean, onAllow: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(if (granted) AppIcons.Shield else AppIcons.Warning)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_permission),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(if (granted) R.string.settings_permission_allowed else R.string.settings_permission_blocked),
                color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        TextButton(onClick = onAllow) {
            Text(
                stringResource(if (granted) R.string.settings_permission_manage else R.string.settings_permission_allow),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(18.dp))
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val activity = context.findActivity()
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
    }
    if (activity != null) {
        activity.startActivity(intent)
    } else {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun appVersion(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
