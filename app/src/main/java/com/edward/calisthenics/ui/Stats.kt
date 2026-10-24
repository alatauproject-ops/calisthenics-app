package com.edward.calisthenics.ui

import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.data.Measure
import com.edward.calisthenics.data.Session
import com.edward.calisthenics.data.Settings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun Settings.isTrainingDay(date: LocalDate): Boolean = date.dayOfWeek.value in trainingDays

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun AppState.sessionsByDay(): Map<Long, List<Session>> = sessions.groupBy { it.epochDay }

fun Session.totalReps(): Int = entries.sumOf { e ->
    if (Catalog.exercise(e.exerciseId)?.measure == Measure.REPS) e.sets.sum() else 0
}

fun Session.totalHoldSec(): Int = entries.sumOf { e ->
    if (Catalog.exercise(e.exerciseId)?.measure == Measure.HOLD) e.sets.sum() else 0
}

/** Сколько недель подряд (включая текущую или прошлую) была хотя бы одна тренировка. */
fun AppState.weekStreak(today: LocalDate = LocalDate.now()): Int {
    val weeks = sessions.map { LocalDate.ofEpochDay(it.epochDay).weekStart() }.toSet()
    var week = today.weekStart()
    if (week !in weeks) week = week.minusWeeks(1)
    var count = 0
    while (week in weeks) {
        count++
        week = week.minusWeeks(1)
    }
    return count
}

/** Лучший подход по каждому упражнению. */
fun AppState.personalBests(): List<Pair<String, Int>> =
    sessions.flatMap { it.entries }
        .filter { it.sets.isNotEmpty() }
        .groupBy { it.exerciseId }
        .map { (id, logs) -> id to logs.maxOf { it.sets.max() } }
        .sortedBy { (id, _) -> Catalog.exercise(id)?.let { Catalog.chains.indexOfFirst { c -> c.id == it.chainId } * 100 + it.step } ?: 0 }
