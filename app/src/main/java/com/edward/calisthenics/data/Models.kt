package com.edward.calisthenics.data

import kotlinx.serialization.Serializable

/** Как считается упражнение: повторения или удержание в секундах. */
enum class Measure { REPS, HOLD }

enum class Level(val title: String, val sets: Int) {
    BEGINNER("Новичок", 3),
    INTERMEDIATE("Средний", 3),
    ADVANCED("Продвинутый", 4),
}

data class Exercise(
    val id: String,
    val chainId: String,
    val step: Int,
    val name: String,
    val measure: Measure,
    /** Цель на подход, с которой начинаем эту ступень. */
    val startTarget: Int,
    /** Цель, после выполнения которой открывается следующая ступень. */
    val goalTarget: Int,
    val cues: String,
)

data class Chain(
    val id: String,
    val title: String,
    val emoji: String,
    val steps: List<Exercise>,
    /** Стартовая ступень для каждого уровня: новичок, средний, продвинутый. */
    val levelStarts: List<Int>,
)

/** Текущая ступень и цель пользователя в цепочке прогрессий. */
@Serializable
data class ChainProgress(
    val step: Int,
    val target: Int,
)

@Serializable
data class ExerciseLog(
    val exerciseId: String,
    val target: Int,
    val sets: List<Int>,
)

@Serializable
data class Session(
    val id: Long,
    val epochDay: Long,
    val startedAt: Long,
    val durationSec: Int,
    val title: String,
    val entries: List<ExerciseLog>,
)

@Serializable
data class Settings(
    val level: Level = Level.BEGINNER,
    /** Дни недели 1..7 (понедельник = 1). */
    val trainingDays: Set<Int> = setOf(1, 3, 5),
    val restSec: Int = 90,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val onboarded: Boolean = false,
)

@Serializable
data class AppState(
    val settings: Settings = Settings(),
    val progress: Map<String, ChainProgress> = emptyMap(),
    val sessions: List<Session> = emptyList(),
)

/** Пункт тренировки: упражнение, цель и количество подходов (0 = без ограничения). */
data class PlanItem(
    val exercise: Exercise,
    val target: Int,
    val sets: Int,
)

data class WorkoutPlan(
    val title: String,
    val items: List<PlanItem>,
    val free: Boolean = false,
)
