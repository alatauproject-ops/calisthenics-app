package com.edward.calisthenics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.data.ChainProgress
import com.edward.calisthenics.data.ExerciseLog
import com.edward.calisthenics.data.Level
import com.edward.calisthenics.data.Measure
import com.edward.calisthenics.data.PlanItem
import com.edward.calisthenics.data.Session
import com.edward.calisthenics.data.Settings
import com.edward.calisthenics.data.Store
import com.edward.calisthenics.data.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

const val TITLE_A = "Тренировка A"
const val TITLE_B = "Тренировка B"

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app.filesDir)
    private val _state = MutableStateFlow(store.load() ?: AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    /** Тренировка, которая сейчас идёт (null, если не идёт). */
    private val _activeWorkout = MutableStateFlow<WorkoutPlan?>(null)
    val activeWorkout: StateFlow<WorkoutPlan?> = _activeWorkout.asStateFlow()

    private fun mutate(block: (AppState) -> AppState) {
        _state.value = block(_state.value)
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) { store.save(snapshot) }
    }

    fun progressFor(s: AppState, chainId: String): ChainProgress =
        s.progress[chainId] ?: Catalog.defaultProgress(s.settings.level).getValue(chainId)

    fun nextWorkoutTitle(s: AppState): String {
        val last = s.sessions.lastOrNull { it.title == TITLE_A || it.title == TITLE_B }
        return if (last?.title == TITLE_A) TITLE_B else TITLE_A
    }

    fun planFor(s: AppState, title: String): WorkoutPlan {
        val chains = if (title == TITLE_B) Catalog.workoutB else Catalog.workoutA
        val items = chains.map { id ->
            val chain = Catalog.chain(id)!!
            val p = progressFor(s, id)
            val ex = chain.steps[p.step.coerceIn(0, chain.steps.lastIndex)]
            PlanItem(ex, p.target, s.settings.level.sets)
        }
        return WorkoutPlan(title, items)
    }

    fun startPlanned(title: String) {
        _activeWorkout.value = planFor(_state.value, title)
    }

    fun startFree(exerciseId: String) {
        val ex = Catalog.exercise(exerciseId) ?: return
        val p = progressFor(_state.value, ex.chainId)
        val target = if (p.step == ex.step) p.target else ex.startTarget
        _activeWorkout.value = WorkoutPlan("Свободная: ${ex.name}", listOf(PlanItem(ex, target, 0)), free = true)
    }

    fun cancelWorkout() {
        _activeWorkout.value = null
    }

    /**
     * Сохраняет тренировку и двигает прогрессии.
     * Возвращает список сообщений о повышении цели или новой ступени.
     */
    fun finishWorkout(plan: WorkoutPlan, logs: List<ExerciseLog>, startedAt: Long, durationSec: Int): List<String> {
        val messages = mutableListOf<String>()
        val nonEmpty = logs.filter { it.sets.isNotEmpty() }
        if (nonEmpty.isEmpty()) {
            _activeWorkout.value = null
            return messages
        }
        mutate { s ->
            val progress = s.progress.toMutableMap()
            if (!plan.free) {
                for (item in plan.items) {
                    val log = nonEmpty.firstOrNull { it.exerciseId == item.exercise.id } ?: continue
                    val completed = log.sets.size >= item.sets && log.sets.all { it >= item.target }
                    if (!completed) continue
                    val ex = item.exercise
                    val chain = Catalog.chain(ex.chainId)!!
                    val inc = if (ex.measure == Measure.HOLD) 5 else 1
                    val newTarget = item.target + inc
                    if (newTarget > ex.goalTarget && ex.step < chain.steps.lastIndex) {
                        val next = chain.steps[ex.step + 1]
                        progress[ex.chainId] = ChainProgress(ex.step + 1, next.startTarget)
                        messages += "Новая ступень: ${next.name} 🎉"
                    } else {
                        progress[ex.chainId] = ChainProgress(ex.step, newTarget)
                        messages += "${ex.name}: цель теперь ${formatValue(ex.measure, newTarget)}"
                    }
                }
            }
            val session = Session(
                id = System.currentTimeMillis(),
                epochDay = LocalDate.now().toEpochDay(),
                startedAt = startedAt,
                durationSec = durationSec,
                title = plan.title,
                entries = nonEmpty,
            )
            s.copy(progress = progress, sessions = s.sessions + session)
        }
        _activeWorkout.value = null
        return messages
    }

    fun deleteSession(id: Long) = mutate { s -> s.copy(sessions = s.sessions.filterNot { it.id == id }) }

    fun updateSettings(block: (Settings) -> Settings) = mutate { s -> s.copy(settings = block(s.settings)) }

    fun completeOnboarding(level: Level, days: Set<Int>) = mutate { s ->
        s.copy(
            settings = s.settings.copy(level = level, trainingDays = days, onboarded = true),
            progress = Catalog.defaultProgress(level),
        )
    }

    fun resetProgressions() = mutate { s -> s.copy(progress = Catalog.defaultProgress(s.settings.level)) }

    fun setStep(chainId: String, step: Int) = mutate { s ->
        val chain = Catalog.chain(chainId) ?: return@mutate s
        val st = step.coerceIn(0, chain.steps.lastIndex)
        s.copy(progress = s.progress + (chainId to ChainProgress(st, chain.steps[st].startTarget)))
    }

    fun adjustTarget(chainId: String, delta: Int) = mutate { s ->
        val p = progressFor(s, chainId)
        s.copy(progress = s.progress + (chainId to p.copy(target = (p.target + delta).coerceAtLeast(1))))
    }

    fun resetAll() = mutate { AppState() }
}

fun formatValue(measure: Measure, value: Int): String =
    if (measure == Measure.HOLD) formatSeconds(value) else "$value"

fun formatSeconds(sec: Int): String =
    if (sec >= 60) "%d:%02d".format(sec / 60, sec % 60) else "$sec с"
