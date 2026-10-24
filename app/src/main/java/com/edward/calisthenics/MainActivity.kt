package com.edward.calisthenics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edward.calisthenics.ui.AppTheme
import com.edward.calisthenics.ui.CalendarScreen
import com.edward.calisthenics.ui.OnboardingScreen
import com.edward.calisthenics.ui.ProgramScreen
import com.edward.calisthenics.ui.ProgressScreen
import com.edward.calisthenics.ui.SettingsScreen
import com.edward.calisthenics.ui.TodayScreen
import com.edward.calisthenics.ui.WorkoutScreen
import com.edward.calisthenics.ui.durationText

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppTheme { App(vm) } }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Сегодня", Icons.Filled.Home),
    CALENDAR("Календарь", Icons.Filled.DateRange),
    PROGRESS("Прогресс", Icons.Filled.Star),
    PROGRAM("Программа", Icons.AutoMirrored.Filled.List),
    SETTINGS("Настройки", Icons.Filled.Settings),
}

private class Summary(val title: String, val sets: Int, val durationSec: Int, val messages: List<String>)

@Composable
private fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val active by vm.activeWorkout.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<Summary?>(null) }

    val context = LocalContext.current
    val feedback = remember { Feedback(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    feedback.sound = state.settings.sound
    feedback.vibration = state.settings.vibration

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        val workout = active
        when {
            !state.settings.onboarded -> OnboardingScreen { level, days -> vm.completeOnboarding(level, days) }
            workout != null -> WorkoutScreen(
                plan = workout,
                restSec = state.settings.restSec,
                feedback = feedback,
                onFinish = { logs, startedAt, duration ->
                    val messages = vm.finishWorkout(workout, logs, startedAt, duration)
                    val sets = logs.sumOf { it.sets.size }
                    if (sets > 0) summary = Summary(workout.title, sets, duration, messages)
                },
                onCancel = { vm.cancelWorkout() },
            )
            else -> Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, windowInsets = androidx.compose.foundation.layout.WindowInsets(0)) {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, t.title) },
                                label = { Text(t.title, maxLines = 1) },
                            )
                        }
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    when (Tab.entries[tab]) {
                        Tab.TODAY -> TodayScreen(state, vm, onQuickCounter = { tab = Tab.PROGRAM.ordinal })
                        Tab.CALENDAR -> CalendarScreen(state, vm)
                        Tab.PROGRESS -> ProgressScreen(state, vm)
                        Tab.PROGRAM -> ProgramScreen(state, vm)
                        Tab.SETTINGS -> SettingsScreen(state, vm)
                    }
                }
            }
        }
    }

    summary?.let { s ->
        AlertDialog(
            onDismissRequest = { summary = null },
            title = { Text("Тренировка сохранена ✓") },
            text = {
                Column {
                    Text("${s.title}: ${s.sets} подходов за ${durationText(s.durationSec)}.")
                    s.messages.forEach { Text("• $it", fontWeight = FontWeight.SemiBold) }
                    if (s.messages.isEmpty()) {
                        Text("Выполните все подходы на цель, чтобы она выросла в следующий раз.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { summary = null }) { Text("Отлично") } },
        )
    }
}
