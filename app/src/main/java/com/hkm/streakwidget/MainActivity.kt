package com.hkm.streakwidget

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hkm.streakwidget.data.localizedFor
import com.hkm.streakwidget.ui.AppScreen
import com.hkm.streakwidget.ui.StreakViewModel
import com.hkm.streakwidget.ui.screens.MainScreen
import com.hkm.streakwidget.ui.theme.StreakWidgetTheme
import com.hkm.streakwidget.widget.StreakAppWidgetProvider

class MainActivity : ComponentActivity() {
    private val viewModel: StreakViewModel by viewModels()
    private val widgetDataChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == StreakAppWidgetProvider.ACTION_WIDGET_DATA_CHANGED) viewModel.loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        routeIntent(intent)
        setContent {
            val language = viewModel.appLanguage.value
            val baseContext = LocalContext.current
            val localizedContext = remember(language, baseContext) { baseContext.localizedFor(language) }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalResources provides localizedContext.resources
            ) {
                StreakWidgetTheme(mode = viewModel.themeMode.value) { MainScreen(viewModel) }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    private fun routeIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_TODAY -> viewModel.requestScreen(AppScreen.HOME)
            ACTION_OPEN_INSIGHTS -> viewModel.requestScreen(AppScreen.INSIGHTS)
        }
        intent?.getStringExtra(StreakAppWidgetProvider.EXTRA_HABIT_ID)?.let(viewModel::focusHabitById)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            widgetDataChangedReceiver,
            IntentFilter(StreakAppWidgetProvider.ACTION_WIDGET_DATA_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(widgetDataChangedReceiver)
        super.onStop()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
    }

    companion object {
        const val ACTION_OPEN_TODAY = "com.hkm.streakwidget.shortcut.TODAY"
        const val ACTION_OPEN_INSIGHTS = "com.hkm.streakwidget.shortcut.INSIGHTS"
    }
}
