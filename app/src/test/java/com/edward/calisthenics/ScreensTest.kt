package com.edward.calisthenics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.ui.MoveFigure
import com.edward.calisthenics.ui.Muted
import com.edward.calisthenics.ui.Surface2
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Exercises
import com.edward.calisthenics.data.Moves
import com.edward.calisthenics.data.Program
import com.edward.calisthenics.data.Session
import com.edward.calisthenics.data.Settings
import com.edward.calisthenics.data.WeightEntry
import com.edward.calisthenics.ui.AppTheme
import com.edward.calisthenics.ui.Bg
import com.edward.calisthenics.ui.ExerciseDetailScreen
import com.edward.calisthenics.ui.FinishScreen
import com.edward.calisthenics.ui.LibraryScreen
import com.edward.calisthenics.ui.NutritionScreen
import com.edward.calisthenics.ui.OnboardingScreen
import com.edward.calisthenics.ui.PlayerResult
import com.edward.calisthenics.ui.PlayerScreen
import com.edward.calisthenics.ui.ProgramScreen
import com.edward.calisthenics.ui.ProgressScreen
import com.edward.calisthenics.ui.TodayScreen
import com.edward.calisthenics.ui.WorkoutPreviewScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Скриншоты экранов для визуальной проверки (Paparazzi, без устройства). */
class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.NoActionBar")

    private val start = LocalDate.of(2026, 11, 2)
    private val today = start.plusDays(8)

    private val state = AppState(
        settings = Settings(startEpochDay = start.toEpochDay(), onboarded = true),
        sessions = listOf(
            Session(1, start.toEpochDay(), 0, 1500, "w1d0", "Всё тело A", 260, true, emptyList()),
            Session(2, start.plusDays(1).toEpochDay(), 0, 2400, "w1d1", "Пляж: ходьба и кор", 310, true, emptyList()),
            Session(3, start.plusDays(3).toEpochDay(), 0, 1600, "w1d2", "Всё тело B", 270, true, emptyList()),
            Session(4, start.plusDays(5).toEpochDay(), 0, 2100, "w1d3", "Пляж: интервальная ходьба", 290, true, emptyList()),
            Session(5, start.plusDays(7).toEpochDay(), 0, 1550, "w2d0", "Всё тело A", 262, true, emptyList()),
        ),
        weights = listOf(
            WeightEntry(start.toEpochDay(), 100.0),
            WeightEntry(start.plusDays(4).toEpochDay(), 98.9),
            WeightEntry(start.plusDays(8).toEpochDay(), 98.1),
        ),
        habits = mapOf(today.toEpochDay() to listOf("steps", "water")),
    )

    @Before
    fun loadMoves() {
        val f = listOf(File("src/main/assets/moves.json"), File("app/src/main/assets/moves.json")).first { it.exists() }
        Moves.loadText(f.readText())
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            AppTheme { Box(Modifier.fillMaxSize().background(Bg)) { content() } }
        }
    }

    @Test
    fun today() = shot("today") {
        TodayScreen(state, {}, {}, {}, {}, {}, {}, today = today)
    }

    @Test
    fun preview() = shot("preview") {
        val w = Program.workout(2, 2, state.settings, state.swaps)
        WorkoutPreviewScreen(state, w, false, "ср, 11 ноября", {}, {}, {}, { _, _ -> }, {})
    }

    @Test
    fun previewBeach() = shot("preview_beach") {
        val w = Program.workout(6, 1, state.settings, state.swaps)
        WorkoutPreviewScreen(state, w, false, "вт, 10 декабря", {}, {}, {}, { _, _ -> }, {})
    }

    @Test
    fun program() = shot("program") { ProgramScreen(state) {} }

    @Test
    fun library() = shot("library") { LibraryScreen {} }

    @Test
    fun detail() = shot("detail") { ExerciseDetailScreen("pushup", {}, {}, {}) }

    @Test
    fun progress() = shot("progress") { ProgressScreen(state, {}, {}, {}) }

    @Test
    fun nutrition() = shot("nutrition") { NutritionScreen(state) }

    @Test
    fun onboarding() = shot("onboarding") { OnboardingScreen(AppState()) { _, _ -> } }

    @Test
    fun playerReps() = shot("player_reps") {
        PlayerScreen(Program.single(Exercises.get("pushup")), 100.0, Feedback(paparazzi.context), {}, {})
    }

    @Test
    fun playerTime() = shot("player_time") {
        PlayerScreen(Program.single(Exercises.get("plank")), 100.0, Feedback(paparazzi.context), {}, {})
    }

    @Test
    fun finish() = shot("finish") {
        FinishScreen("Всё тело A", PlayerResult(emptyList(), 0, 1520, 265, true, 19, 20, 154), 5, {}, {})
    }
}

/** Все анимации техники: по два кадра на движение. */
class FiguresTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.NoActionBar")

    @Before
    fun loadMoves() {
        val f = listOf(File("src/main/assets/moves.json"), File("app/src/main/assets/moves.json")).first { it.exists() }
        Moves.loadText(f.readText())
    }

    private fun sheet(name: String, ids: List<String>) = paparazzi.snapshot(name) {
        AppTheme {
            Column(Modifier.fillMaxSize().background(Bg).padding(4.dp)) {
                ids.forEach { id ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (ph in listOf(0f, 0.25f, 0.5f)) {
                            Box(Modifier.weight(1f).fillMaxHeight().padding(2.dp).background(Surface2)) {
                                MoveFigure(id, Modifier.fillMaxSize(), phase = ph)
                                Text(id, fontSize = 8.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }

    private val all = Exercises.all.map { it.anim }.distinct()

    @Test fun figures1() = sheet("figures1", all.take(8))
    @Test fun figures2() = sheet("figures2", all.drop(8).take(8))
    @Test fun figures3() = sheet("figures3", all.drop(16).take(8))
    @Test fun figures4() = sheet("figures4", all.drop(24).take(8))
    @Test fun figures5() = sheet("figures5", all.drop(32).take(9))
}
