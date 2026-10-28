package com.edward.calisthenics.data

import java.time.LocalDate
import kotlin.math.roundToInt

enum class Kind(val title: String) {
    STRENGTH("Сила"),
    CIRCUIT("Круговая"),
    BEACH("Пляж"),
    HIIT("Интервалы"),
    CORE("Кор"),
    SINGLE("Упражнение"),
}

/** Пункт блока: упражнение и цель (повторения или секунды). */
data class Item(val exercise: Exercise, val value: Int, val originalId: String = exercise.id)

data class Block(
    val title: String,
    val rounds: Int,
    val items: List<Item>,
    /** Отдых между упражнениями, сек. */
    val restItem: Int,
    /** Отдых между кругами, сек. */
    val restRound: Int,
)

data class Workout(
    val key: String,
    val title: String,
    val kind: Kind,
    val note: String,
    val blocks: List<Block>,
    val week: Int = 0,
    val day: Int = 0,
)

/** Ближайшая тренировка для экрана «Сегодня». */
data class NextUp(val week: Int, val day: Int, val date: LocalDate, val isToday: Boolean, val overdue: Boolean)

data class Phase(val title: String, val weeks: IntRange, val goal: String, val steps: Int)

/** Шаг плеера: работа или отдых. */
sealed class Step {
    data class Work(val item: Item, val block: Block, val round: Int, val index: Int) : Step()
    data class Rest(val seconds: Int, val title: String) : Step()
}

object Program {
    const val WEEKS = 12
    const val DAYS = 4

    val phases = listOf(
        Phase(
            "Основа", 1..4,
            "Учимся технике, укрепляем суставы и связки, приучаем тело к нагрузке. Без прыжков: при весе 100 кг колени важнее скорости.",
            8000,
        ),
        Phase(
            "Жиросжигание", 5..8,
            "Больше кругов и интервалов, появляется бег по пляжу и первые прыжки. Пульс выше, отдых короче.",
            10000,
        ),
        Phase(
            "Рельеф", 9..12,
            "Сложные варианты упражнений, ускорения на песке и HIIT 45/15. Закрепляем результат и выходим на цель.",
            12000,
        ),
    )

    fun phase(week: Int): Phase = phases.first { week.coerceIn(1, WEEKS) in it.weeks }

    // ---------- расписание ----------

    fun weekOf(s: Settings, day: LocalDate): Int = ((day.toEpochDay() - s.startEpochDay) / 7).toInt() + 1

    fun dateOf(s: Settings, week: Int, day: Int): LocalDate {
        val dow = s.trainingDays.sorted().getOrElse(day) { day + 1 }
        return LocalDate.ofEpochDay(s.startEpochDay + (week - 1) * 7L + (dow - 1))
    }

    fun key(week: Int, day: Int) = "w${week}d$day"

    /** Первая невыполненная тренировка текущей недели с датой не позже сегодня, иначе ближайшая будущая. */
    fun nextUp(s: AppState, today: LocalDate = LocalDate.now()): NextUp? {
        val done = s.doneKeys
        val week = weekOf(s.settings, today).coerceIn(1, WEEKS)
        for (w in week..WEEKS) {
            for (d in 0 until DAYS) {
                if (key(w, d) in done) continue
                val date = dateOf(s.settings, w, d)
                if (w == week && date.isBefore(today)) return NextUp(w, d, date, isToday = false, overdue = true)
                if (!date.isBefore(today)) return NextUp(w, d, date, isToday = date == today, overdue = false)
            }
        }
        return null
    }

    // ---------- сборка тренировки ----------

    private class Builder(val s: Settings, val swaps: Map<String, String>, val k: Int) {
        fun reps(base: Int, inc: Int) = ((base + inc * k) * s.level.factor).roundToInt().coerceAtLeast(1)
        fun sec(base: Int, inc: Int) = (((base + inc * k) * s.level.factor) / 5.0).roundToInt().coerceAtLeast(1) * 5
        fun min(base: Int, inc: Int) = (base + inc * k) * 60

        fun item(id: String, value: Int): Item {
            val base = Exercises.get(id)
            var ex = Exercises.available(id, s)
            swaps[id]?.let { Exercises.find(it) }?.let { if (Exercises.usable(it, s)) ex = it }
            val v = when {
                ex.measure == base.measure -> value
                ex.measure == Measure.TIME -> (value * base.secPerRep / 5).roundToInt().coerceAtLeast(2) * 5
                else -> (value / ex.secPerRep).roundToInt().coerceAtLeast(3)
            }
            return Item(ex, v, id)
        }

        fun r(id: String, base: Int, inc: Int) = item(id, reps(base, inc))
        fun t(id: String, base: Int, inc: Int) = item(id, sec(base, inc))
        fun m(id: String, base: Int, inc: Int) = item(id, min(base, inc))
        fun fixed(id: String, seconds: Int) = item(id, seconds)
    }

    private fun Builder.warmup(jumps: Boolean) = Block(
        "Разминка", 1,
        listOf(
            fixed("march", 45),
            fixed("arm_circles", 30),
            fixed(if (jumps) "jumping_jacks" else "step_jacks", 40),
            item("squat", 10),
            item("inchworm", 4),
        ),
        restItem = 10, restRound = 0,
    )

    private fun Builder.cooldown() = Block(
        "Заминка и растяжка", 1,
        listOf(
            fixed("hamstring_stretch", 40),
            fixed("quad_stretch", 40),
            fixed("child_pose", 40),
            fixed("cobra", 30),
        ),
        restItem = 5, restRound = 0,
    )

    fun workout(week: Int, day: Int, s: Settings, swaps: Map<String, String>): Workout {
        val w = week.coerceIn(1, WEEKS)
        val b = Builder(s, swaps, (w - 1) % 4)
        val key = key(w, day)
        return when ((w - 1) / 4) {
            0 -> phase1(b, day, key, w)
            1 -> phase2(b, day, key, w)
            else -> phase3(b, day, key, w)
        }
    }

    private fun phase1(b: Builder, day: Int, key: String, w: Int): Workout = with(b) {
        when (day) {
            0 -> Workout(
                key, "Всё тело A", Kind.CIRCUIT,
                "Круговая тренировка: выполните упражнения подряд, отдых в конце круга.",
                listOf(
                    warmup(false),
                    Block(
                        "Круговая", 3,
                        listOf(r("squat", 12, 1), r("incline_pushup", 8, 1), r("glute_bridge", 12, 2), r("australian_row", 6, 1), t("plank", 20, 5)),
                        restItem = 20, restRound = 90,
                    ),
                    cooldown(),
                ), w, day,
            )
            1 -> Workout(
                key, "Пляж: ходьба и кор", Kind.BEACH,
                "Быстрая ходьба вдоль моря, потом короткий блок на коврике прямо на берегу.",
                listOf(
                    Block("Ходьба", 1, listOf(m("brisk_walk", 25, 5)), 0, 0),
                    Block(
                        "Кор на коврике", 2,
                        listOf(r("dead_bug", 8, 2), t("superman", 15, 5), t("side_plank", 20, 5), r("crunch", 10, 2)),
                        restItem = 15, restRound = 60,
                    ),
                    cooldown(),
                ), w, day,
            )
            2 -> Workout(
                key, "Всё тело B", Kind.CIRCUIT,
                "Вторая круговая: ноги, трицепс, спина и кор.",
                listOf(
                    warmup(false),
                    Block(
                        "Круговая", 3,
                        listOf(r("reverse_lunge", 12, 2), r("knee_pushup", 8, 1), t("squat_hold", 20, 5), r("bench_dip", 6, 1), t("dead_hang", 15, 5), t("mountain_climber", 20, 5)),
                        restItem = 20, restRound = 90,
                    ),
                    cooldown(),
                ), w, day,
            )
            else -> Workout(
                key, "Пляж: интервальная ходьба", Kind.BEACH,
                "Чередуйте быструю и очень быструю ходьбу. Таймер подскажет, когда менять темп.",
                listOf(
                    Block("Разогрев", 1, listOf(fixed("walk", 300)), 0, 0),
                    Block("Интервалы", 5 + (w - 1) % 4, listOf(fixed("brisk_walk", 120), fixed("jog", 30)), 0, 0),
                    Block("Ноги на песке", 2, listOf(r("calf_raise", 15, 2), r("glute_bridge", 15, 2)), restItem = 15, restRound = 45),
                    cooldown(),
                ), w, day,
            )
        }
    }

    private fun phase2(b: Builder, day: Int, key: String, w: Int): Workout = with(b) {
        when (day) {
            0 -> Workout(
                key, "Сила: верх и ноги", Kind.STRENGTH,
                "Четыре круга силовых упражнений. Если отжимания не идут, замените на облегчённый вариант.",
                listOf(
                    warmup(false),
                    Block(
                        "Силовая круговая", 4,
                        listOf(r("pushup", 6, 1), r("squat_jump", 8, 1), r("negative_pullup", 3, 1), r("reverse_lunge", 16, 2), r("pike_pushup", 5, 1), t("hollow_hold", 20, 5)),
                        restItem = 20, restRound = 90,
                    ),
                    cooldown(),
                ), w, day,
            )
            1 -> Workout(
                key, "Пляж: бег и ходьба", Kind.BEACH,
                "Интервалы лёгкого бега и быстрой ходьбы по плотному песку у воды.",
                listOf(
                    Block("Разогрев", 1, listOf(fixed("brisk_walk", 300)), 0, 0),
                    Block("Интервалы", 6, listOf(b.fixed("jog", 60 + 15 * ((w - 1) % 4)), fixed("brisk_walk", 90)), 0, 0),
                    Block("Заминка", 1, listOf(fixed("walk", 300)), 0, 0),
                    cooldown(),
                ), w, day,
            )
            2 -> Workout(
                key, "Жиросжигающая круговая", Kind.HIIT,
                "Работа на время: делайте в своём темпе, но без остановок.",
                listOf(
                    warmup(true),
                    Block(
                        "Круговая на время", if ((w - 1) % 4 >= 2) 4 else 3,
                        listOf(t("burpee_low", 30, 5), t("squat", 30, 5), t("mountain_climber", 30, 5), t("high_knees", 30, 5), t("glute_bridge", 30, 5), t("superman", 30, 5)),
                        restItem = 20, restRound = 60,
                    ),
                    cooldown(),
                ), w, day,
            )
            else -> Workout(
                key, "Кор и прогулка", Kind.CORE,
                "Пресс и спина на коврике, затем длинная прогулка вдоль моря.",
                listOf(
                    Block(
                        "Кор", 3,
                        listOf(t("hollow_hold", 20, 5), r("dead_bug", 10, 2), r("leg_raises", 10, 1), t("side_plank", 20, 5), r("superman_pull", 12, 1)),
                        restItem = 15, restRound = 60,
                    ),
                    Block("Прогулка", 1, listOf(m("brisk_walk", 30, 5)), 0, 0),
                    cooldown(),
                ), w, day,
            )
        }
    }

    private fun phase3(b: Builder, day: Int, key: String, w: Int): Workout = with(b) {
        val k = (w - 1) % 4
        when (day) {
            0 -> Workout(
                key, "Сила: толкай и тяни", Kind.STRENGTH,
                "Самые сложные варианты. Держите технику: лучше меньше повторов, но чисто.",
                listOf(
                    warmup(true),
                    Block(
                        "Силовая круговая", 4,
                        listOf(r("pushup", 10, 1), r("pullup", 3, 1), r("decline_pushup", 6, 1), r("australian_row", 8, 1), r("bench_dip", 10, 1), t("hollow_hold", 30, 5)),
                        restItem = 20, restRound = 90,
                    ),
                    cooldown(),
                ), w, day,
            )
            1 -> Workout(
                key, "Пляж: ускорения", Kind.BEACH,
                "Ускорения по песку с полным восстановлением шагом. Лучше утром до жары.",
                listOf(
                    Block("Разогрев", 1, listOf(fixed("brisk_walk", 300), fixed("jog", 300)), 0, 0),
                    Block("Ускорения", 8 + k, listOf(fixed("sprint", 20), fixed("walk", 70)), 0, 0),
                    Block("Заминка", 1, listOf(fixed("jog", 180), fixed("walk", 180)), 0, 0),
                    cooldown(),
                ), w, day,
            )
            2 -> Workout(
                key, "HIIT 45/15", Kind.HIIT,
                "Короткий отдых, высокий пульс. Если тяжело, берите вариант полегче, но не останавливайтесь.",
                listOf(
                    warmup(true),
                    Block(
                        "HIIT", 4,
                        listOf(t("burpee", 35, 5), t("jump_lunge", 35, 5), t("squat_jump", 35, 5), t("mountain_climber", 35, 5), t("high_knees", 35, 5), t("plank", 35, 5)),
                        restItem = 15, restRound = 60,
                    ),
                    cooldown(),
                ), w, day,
            )
            else -> Workout(
                key, "Ноги, кор и длинная прогулка", Kind.CORE,
                "Объёмная работа на ноги и пресс, затем прогулка 40+ минут.",
                listOf(
                    Block(
                        "Ноги и кор", 3,
                        listOf(r("squat", 20, 2), r("reverse_lunge", 20, 2), r("glute_bridge", 20, 2), r("calf_raise", 20, 2), t("side_plank", 30, 5), r("hanging_knee_raise", 10, 1)),
                        restItem = 15, restRound = 60,
                    ),
                    Block("Прогулка", 1, listOf(m("brisk_walk", 40, 5)), 0, 0),
                    cooldown(),
                ), w, day,
            )
        }
    }

    /** Отдельное упражнение: три подхода. */
    fun single(ex: Exercise): Workout {
        val value = when {
            ex.fixedVolume -> 300
            ex.measure == Measure.TIME -> 30
            else -> 10
        }
        return Workout(
            "free:${ex.id}", ex.name, Kind.SINGLE, "Три подхода с отдыхом.",
            listOf(Block("Подходы", if (ex.fixedVolume) 1 else 3, listOf(Item(ex, value)), restItem = 0, restRound = 60)),
        )
    }

    // ---------- шаги плеера и оценки ----------

    fun steps(w: Workout): List<Step> {
        val out = mutableListOf<Step>()
        var index = 0
        w.blocks.forEachIndexed { bi, block ->
            if (bi > 0 && out.isNotEmpty() && out.last() !is Step.Rest) out += Step.Rest(20, "Переход: ${block.title.lowercase()}")
            for (round in 1..block.rounds) {
                block.items.forEachIndexed { ii, item ->
                    out += Step.Work(item, block, round, index++)
                    val lastItem = ii == block.items.lastIndex
                    if (!lastItem && block.restItem > 0) out += Step.Rest(block.restItem, "Отдых")
                    if (lastItem && round < block.rounds && block.restRound > 0) out += Step.Rest(block.restRound, "Отдых между кругами")
                }
            }
        }
        if (out.lastOrNull() is Step.Rest) out.removeAt(out.lastIndex)
        return out
    }

    fun workSeconds(item: Item): Double =
        if (item.exercise.measure == Measure.TIME) item.value.toDouble() else item.value * item.exercise.secPerRep

    fun durationSec(w: Workout): Int = steps(w).sumOf { st ->
        when (st) {
            is Step.Work -> workSeconds(st.item) + 4
            is Step.Rest -> st.seconds.toDouble()
        }
    }.roundToInt()

    fun kcal(w: Workout, weightKg: Double): Int = steps(w).sumOf { st ->
        when (st) {
            is Step.Work -> st.item.exercise.met * weightKg * workSeconds(st.item) / 3600.0
            is Step.Rest -> 2.0 * weightKg * st.seconds / 3600.0
        }
    }.roundToInt()

    fun kcalFor(ex: Exercise, value: Int, weightKg: Double): Double {
        val sec = if (ex.measure == Measure.TIME) value.toDouble() else value * ex.secPerRep
        return ex.met * weightKg * sec / 3600.0
    }

    /** Все упражнения тренировки без повторов, в порядке появления. */
    fun distinctExercises(w: Workout): List<Exercise> =
        w.blocks.flatMap { b -> b.items.map { it.exercise } }.distinctBy { it.id }
}
