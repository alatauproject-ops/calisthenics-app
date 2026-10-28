package com.edward.calisthenics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.ExerciseLog
import com.edward.calisthenics.data.Profile
import com.edward.calisthenics.data.Program
import com.edward.calisthenics.data.Session
import com.edward.calisthenics.data.Settings
import com.edward.calisthenics.data.Store
import com.edward.calisthenics.data.WeightEntry
import com.edward.calisthenics.data.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Ссылка на тренировку: из программы или отдельное упражнение. */
data class WorkoutRef(val week: Int = 0, val day: Int = 0, val exerciseId: String? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app.filesDir)
    private val _state = MutableStateFlow(store.load() ?: AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun mutate(block: (AppState) -> AppState) {
        _state.value = block(_state.value)
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) { store.save(snapshot) }
    }

    fun workout(s: AppState, ref: WorkoutRef): Workout =
        if (ref.exerciseId != null) {
            Program.single(com.edward.calisthenics.data.Exercises.get(ref.exerciseId))
        } else {
            Program.workout(ref.week, ref.day, s.settings, s.swaps)
        }

    fun saveSession(ref: WorkoutRef, w: Workout, logs: List<ExerciseLog>, startedAt: Long, durationSec: Int, kcal: Int, completed: Boolean) {
        if (logs.isEmpty()) return
        mutate { s ->
            val session = Session(
                id = System.currentTimeMillis(),
                epochDay = LocalDate.now().toEpochDay(),
                startedAt = startedAt,
                durationSec = durationSec,
                key = w.key,
                title = w.title,
                kcal = kcal,
                completed = completed && ref.exerciseId == null,
                entries = logs,
            )
            s.copy(sessions = s.sessions + session)
        }
    }

    fun deleteSession(id: Long) = mutate { s -> s.copy(sessions = s.sessions.filterNot { it.id == id }) }

    fun markDone(week: Int, day: Int, done: Boolean) = mutate { s ->
        val key = Program.key(week, day)
        if (done) {
            val w = Program.workout(week, day, s.settings, s.swaps)
            s.copy(
                sessions = s.sessions + Session(
                    System.currentTimeMillis(), LocalDate.now().toEpochDay(), System.currentTimeMillis(),
                    Program.durationSec(w), key, w.title, Program.kcal(w, s.currentWeight), true, emptyList(),
                ),
            )
        } else {
            s.copy(sessions = s.sessions.filterNot { it.key == key })
        }
    }

    fun logWeight(kg: Double, day: LocalDate = LocalDate.now()) = mutate { s ->
        val e = day.toEpochDay()
        s.copy(weights = (s.weights.filterNot { it.epochDay == e } + WeightEntry(e, kg)).sortedBy { it.epochDay })
    }

    fun deleteWeight(epochDay: Long) = mutate { s -> s.copy(weights = s.weights.filterNot { it.epochDay == epochDay }) }

    fun toggleHabit(id: String, day: LocalDate = LocalDate.now()) = mutate { s ->
        val e = day.toEpochDay()
        val cur = s.habits[e].orEmpty()
        val next = if (id in cur) cur - id else cur + id
        s.copy(habits = s.habits + (e to next))
    }

    fun swap(originalId: String, newId: String?) = mutate { s ->
        s.copy(swaps = if (newId == null || newId == originalId) s.swaps - originalId else s.swaps + (originalId to newId))
    }

    fun updateSettings(block: (Settings) -> Settings) = mutate { s -> s.copy(settings = block(s.settings)) }
    fun updateProfile(block: (Profile) -> Profile) = mutate { s -> s.copy(profile = block(s.profile)) }

    fun completeOnboarding(profile: Profile, settings: Settings) = mutate { s ->
        val weights = if (s.weights.isEmpty()) listOf(WeightEntry(LocalDate.now().toEpochDay(), profile.startWeight)) else s.weights
        s.copy(profile = profile, settings = settings.copy(onboarded = true), weights = weights)
    }

    fun resetAll() = mutate { AppState() }

    companion object {
        /** Понедельник, с которого стоит начать: сегодня, если понедельник, иначе следующий. */
        fun nextMonday(today: LocalDate = LocalDate.now()): LocalDate =
            today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

        fun thisMonday(today: LocalDate = LocalDate.now()): LocalDate =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
