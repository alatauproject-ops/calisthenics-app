package com.edward.calisthenics.data

import kotlinx.serialization.Serializable

/** Как считается упражнение: повторения или время. */
enum class Measure { REPS, TIME }

/** Множитель объёма программы. */
enum class Level(val title: String, val factor: Double) {
    EASY("Полегче", 0.8),
    NORMAL("Обычный", 1.0),
    HARD("Посложнее", 1.2),
}

@Serializable
data class Profile(
    val age: Int = 35,
    val heightCm: Int = 180,
    val startWeight: Double = 100.0,
    val goalWeight: Double = 85.0,
)

@Serializable
data class Settings(
    /** Понедельник, с которого начинается 12-недельная программа. */
    val startEpochDay: Long = 0,
    /** Дни тренировок, 1 = понедельник. Ровно четыре. */
    val trainingDays: List<Int> = listOf(1, 2, 4, 6),
    val level: Level = Level.NORMAL,
    val hasBar: Boolean = false,
    val hasBench: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val voice: Boolean = true,
    val onboarded: Boolean = false,
)

@Serializable
data class ExerciseLog(
    val exerciseId: String,
    /** Выполнено в каждом подходе: повторения или секунды. */
    val values: List<Int>,
)

@Serializable
data class Session(
    val id: Long,
    val epochDay: Long,
    val startedAt: Long,
    val durationSec: Int,
    /** Ключ тренировки программы ("w3d1") или "free:<id>". */
    val key: String,
    val title: String,
    val kcal: Int,
    val completed: Boolean,
    val entries: List<ExerciseLog>,
)

@Serializable
data class WeightEntry(val epochDay: Long, val kg: Double)

@Serializable
data class AppState(
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    val sessions: List<Session> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    /** Отмеченные привычки по дням. */
    val habits: Map<Long, List<String>> = emptyMap(),
    /** Замены упражнений: исходное упражнение программы -> выбранное. */
    val swaps: Map<String, String> = emptyMap(),
) {
    val currentWeight: Double get() = weights.maxByOrNull { it.epochDay }?.kg ?: profile.startWeight
    val doneKeys: Set<String> get() = sessions.filter { it.completed }.mapTo(HashSet()) { it.key }
}
