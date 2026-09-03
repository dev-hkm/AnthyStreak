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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        intent.getStringExtra("HABIT_ID")?.let(viewModel::focusHabitById)
        setContent { StreakWidgetTheme(mode = viewModel.themeMode.value) { MainScreen(viewModel) } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("HABIT_ID")?.let(viewModel::focusHabitById)
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
}
