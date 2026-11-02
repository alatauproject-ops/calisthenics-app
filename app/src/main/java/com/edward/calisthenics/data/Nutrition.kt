package com.edward.calisthenics.data

import kotlin.math.roundToInt

data class NutritionPlan(
    val bmr: Int,
    val tdee: Int,
    val kcal: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
    val waterL: Double,
    val weeklyLossKg: Double,
)

object Nutrition {
    /** Формула Миффлина — Сан-Жеора (мужчина), активность 1.5, дефицит 25%. */
    fun plan(p: Profile, weightKg: Double): NutritionPlan {
        val bmr = 10 * weightKg + 6.25 * p.heightCm - 5 * p.age + 5
        val tdee = bmr * 1.5
        val kcal = maxOf(tdee * 0.75, bmr * 1.05)
        val protein = 1.8 * p.goalWeight
        val fat = 0.8 * p.goalWeight
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).coerceAtLeast(80.0)
        val water = (weightKg * 0.035 + 0.75)
        val loss = (tdee - kcal) * 7 / 7700
        return NutritionPlan(
            bmr.roundToInt(), tdee.roundToInt(), (kcal / 50).roundToInt() * 50,
            (protein / 5).roundToInt() * 5, (fat / 5).roundToInt() * 5, (carbs / 5).roundToInt() * 5,
            (water * 2).roundToInt() / 2.0, loss,
        )
    }

    val habits = listOf(
        "steps" to "Шаги",
        "water" to "Вода",
        "protein" to "Белок в каждом приёме пищи",
        "nosugar" to "Без сладких напитков и пива",
        "sleep" to "Сон 7+ часов",
    )

    val choose = listOf(
        "Рыба и морепродукты на гриле: cá nướng, mực nướng, tôm",
        "Курица, говядина: gà nướng, bò lúc lắc без сладкого соуса. Свинину (thịt heo) не берём",
        "Яйца, тофу (đậu hũ), йогурт без сахара",
        "Овощи и зелень: rau muống, салаты, свежие травы к фо",
        "Фо и бун с мясом: просите меньше лапши, больше мяса и зелени",
        "Фрукты в первой половине дня: папайя, арбуз, грейпфрут",
    )

    val limit = listOf(
        "Cà phê sữa đá: сгущёнка даёт до 150–200 ккал. Берите cà phê đen или с капелькой молока",
        "Сахарный тростник, смузи, соки, газировка",
        "Пиво: 3 банки это около 450 ккал и аппетит на ночь",
        "Жареное: bánh xèo, спринг-роллы во фритюре, жареный рис",
        "Белый рис порциями больше кулака, баньми каждый день",
        "Манго, дуриан, джекфрут в больших количествах: очень сладкие",
        "Блюда kho и соусы в кафе часто на сахаре: просите không đường (без сахара) или ít ngọt",
    )

    val sampleDay = listOf(
        "Завтрак" to "3 яйца, овощи, кусочек хлеба или небольшая порция риса, кофе без сахара",
        "Обед" to "Рыба или курица на гриле 200 г, рис с кулак, много овощей",
        "Перекус" to "Йогурт без сахара или протеин, фрукт",
        "Ужин" to "Фо или бун: больше мяса и зелени, половина лапши. Или морепродукты с салатом",
    )

    val tips = listOf(
        "Тренируйтесь утром до 8:00 или вечером после 17:00: днём в Муйне слишком жарко.",
        "Пейте воду до, во время и после тренировки. В жару добавьте щепотку соли или электролиты.",
        "Взвешивайтесь утром натощак после туалета, лучше по понедельникам. Смотрите на тренд, а не на один день.",
        "Не голодайте: дефицит больше 30% съедает мышцы и срывает режим.",
        "Белок в каждом приёме пищи держит сытость и сохраняет мышцы.",
    )
}
