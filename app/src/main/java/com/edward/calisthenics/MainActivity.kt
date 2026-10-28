package com.edward.calisthenics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Moves
import com.edward.calisthenics.data.Program
import com.edward.calisthenics.ui.Accent
import com.edward.calisthenics.ui.AppTheme
import com.edward.calisthenics.ui.Bg
import com.edward.calisthenics.ui.ExerciseDetailScreen
import com.edward.calisthenics.ui.FinishScreen
import com.edward.calisthenics.ui.LibraryScreen
import com.edward.calisthenics.ui.Muted
import com.edward.calisthenics.ui.NutritionScreen
import com.edward.calisthenics.ui.OnboardingScreen
import com.edward.calisthenics.ui.PlayerResult
import com.edward.calisthenics.ui.PlayerScreen
import com.edward.calisthenics.ui.ProgramScreen
import com.edward.calisthenics.ui.ProgressScreen
import com.edward.calisthenics.ui.SettingsScreen
import com.edward.calisthenics.ui.Surface1
import com.edward.calisthenics.ui.TodayScreen
import com.edward.calisthenics.ui.WeightDialog
import com.edward.calisthenics.ui.WorkoutPreviewScreen
import com.edward.calisthenics.ui.shortTitle
import com.edward.calisthenics.ui.weekdayShort

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Moves.load(applicationContext)
        setContent { AppTheme { App(vm) } }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Сегодня", Icons.Filled.Home),
    PROGRAM("План", Icons.Filled.CalendarMonth),
    LIBRARY("Упражнения", Icons.Filled.FitnessCenter),
    PROGRESS("Прогресс", Icons.Filled.BarChart),
    FOOD("Питание", Icons.Filled.Restaurant),
}

private sealed class Screen {
    data class Preview(val ref: WorkoutRef) : Screen()
    data class Exercise(val id: String) : Screen()
    data class Player(val ref: WorkoutRef) : Screen()
    data class Finish(val title: String, val result: PlayerResult) : Screen()
    data object Settings : Screen()
}

/** Сколько тренировок программы выполнено подряд с начала, без пропусков. */
private fun streak(state: AppState): Int {
    val done = state.doneKeys
    var n = 0
    for (w in 1..Program.WEEKS) for (d in 0 until Program.DAYS) {
        if (Program.key(w, d) in done) n++ else return n
    }
    return n
}

@Composable
private fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>() }
    var weightDialog by remember { mutableStateOf(false) }
    var foodTab by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current
    val feedback = remember { Feedback(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    feedback.sound = state.settings.sound
    feedback.vibration = state.settings.vibration
    feedback.voice = state.settings.voice

    fun push(s: Screen) = stack.add(s)
    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    val openWorkout: (WorkoutRef) -> Unit = { push(Screen.Preview(it)) }
    val startWorkout: (WorkoutRef) -> Unit = { push(Screen.Player(it)) }

    Box(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        val top = stack.lastOrNull()
        if (!state.settings.onboarded) {
            OnboardingScreen(state) { p, s -> vm.completeOnboarding(p, s) }
        } else when (top) {
            null -> Scaffold(
                containerColor = Bg,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    NavigationBar(containerColor = Surface1, windowInsets = WindowInsets(0)) {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, t.title) },
                                label = { Text(t.title, maxLines = 1, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Accent,
                                    selectedTextColor = Accent,
                                    indicatorColor = Accent.copy(alpha = 0.16f),
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    when (Tab.entries[tab]) {
                        Tab.TODAY -> TodayScreen(
                            state,
                            onOpen = openWorkout,
                            onStart = startWorkout,
                            onSettings = { push(Screen.Settings) },
                            onLogWeight = { weightDialog = true },
                            onToggleHabit = { vm.toggleHabit(it) },
                            onStartThisWeek = { vm.updateSettings { it.copy(startEpochDay = AppViewModel.thisMonday().toEpochDay()) } },
                            onFood = {
                                foodTab = 0
                                tab = Tab.FOOD.ordinal
                            },
                        )
                        Tab.PROGRAM -> ProgramScreen(state, onOpen = openWorkout)
                        Tab.LIBRARY -> LibraryScreen(onExercise = { push(Screen.Exercise(it)) })
                        Tab.PROGRESS -> ProgressScreen(
                            state,
                            onLogWeight = { weightDialog = true },
                            onDeleteWeight = { vm.deleteWeight(it) },
                            onDeleteSession = { vm.deleteSession(it) },
                        )
                        Tab.FOOD -> NutritionScreen(
                            state,
                            tab = foodTab,
                            onTab = { foodTab = it },
                            onToggleShop = { vm.toggleShop(it) },
                            onClearShop = { vm.clearShop() },
                        )
                    }
                }
            }
            is Screen.Preview -> {
                val ref = top.ref
                val workout = vm.workout(state, ref)
                val isProgram = ref.isProgram
                val date = if (isProgram) Program.dateOf(state.settings, ref.week, ref.day) else null
                WorkoutPreviewScreen(
                    state = state,
                    workout = workout,
                    isDone = ref.exerciseId == null && workout.key in state.doneKeys,
                    dateLabel = date?.let { "${it.weekdayShort()}, ${it.shortTitle()}" },
                    onBack = { pop() },
                    onStart = { push(Screen.Player(ref)) },
                    onExercise = { push(Screen.Exercise(it)) },
                    onSwap = { o, n -> vm.swap(o, n) },
                    onToggleDone = when {
                        isProgram -> ({ done: Boolean -> vm.markDone(ref.week, ref.day, done) })
                        ref.daily -> ({ done: Boolean -> vm.markDailyDone(done) })
                        else -> null
                    },
                )
            }
            is Screen.Exercise -> ExerciseDetailScreen(
                id = top.id,
                onBack = { pop() },
                onTry = { push(Screen.Player(WorkoutRef(exerciseId = top.id))) },
                onOpen = { push(Screen.Exercise(it)) },
            )
            is Screen.Player -> {
                val ref = top.ref
                val workout = remember(ref) { vm.workout(state, ref) }
                PlayerScreen(
                    workout = workout,
                    weightKg = state.currentWeight,
                    feedback = feedback,
                    onFinish = { r ->
                        vm.saveSession(ref, workout, r.logs, r.startedAt, r.durationSec, r.kcal, r.completed)
                        pop()
                        // Превью этой тренировки больше не нужно: сразу на итог.
                        if (stack.lastOrNull() is Screen.Preview) pop()
                        push(Screen.Finish(workout.title, r))
                    },
                    onExit = { pop() },
                )
            }
            is Screen.Finish -> FinishScreen(
                title = top.title,
                result = top.result,
                streak = streak(state),
                onLogWeight = { weightDialog = true },
                onDone = {
                    stack.clear()
                    tab = Tab.TODAY.ordinal
                },
            )
            Screen.Settings -> SettingsScreen(state, vm, onBack = { pop() })
        }
        if (top != null && top !is Screen.Player && state.settings.onboarded) {
            BackHandler {
                if (top is Screen.Finish) stack.clear() else pop()
            }
        }
    }

    if (weightDialog) {
        WeightDialog(
            initial = state.currentWeight,
            onSave = {
                vm.logWeight(it)
                weightDialog = false
            },
            onDismiss = { weightDialog = false },
        )
    }
}
