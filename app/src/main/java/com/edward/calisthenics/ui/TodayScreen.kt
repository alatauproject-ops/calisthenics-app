package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.WorkoutRef
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Nutrition
import com.edward.calisthenics.data.Program
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(
    state: AppState,
    onOpen: (WorkoutRef) -> Unit,
    onStart: (WorkoutRef) -> Unit,
    onSettings: () -> Unit,
    onLogWeight: () -> Unit,
    onToggleHabit: (String) -> Unit,
    onStartThisWeek: () -> Unit,
    onFood: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val s = state.settings
    val start = LocalDate.ofEpochDay(s.startEpochDay)
    val rawWeek = Program.weekOf(s, today)
    val week = rawWeek.coerceIn(1, Program.WEEKS)
    val phase = Program.phase(week)
    val next = Program.nextUp(state, today)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(today.longTitle(), style = Caption)
                Text(
                    when {
                        rawWeek < 1 -> "Скоро старт"
                        rawWeek > Program.WEEKS -> "Программа пройдена"
                        else -> "Неделя $week из ${Program.WEEKS}"
                    },
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                )
                if (rawWeek in 1..Program.WEEKS) Text("Фаза «${phase.title}»", color = AccentSoft, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Настройки", tint = Muted) }
        }

        if (rawWeek < 1) {
            val days = ChronoUnit.DAYS.between(today, start)
            Card(color = Surface1) {
                Text("Старт программы: ${start.longTitle().lowercase()}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Осталось дней: $days. Пока можно посмотреть упражнения и попробовать первую тренировку.", color = Muted)
                TextButton(onClick = onStartThisWeek) {
                    Text("Начать уже с этой недели")
                }
            }
        }

        if (next != null) {
            val w = Program.workout(next.week, next.day, s, state.swaps)
            val label = when {
                next.overdue -> "Пропущена · ${next.date.weekdayShort()}, ${next.date.shortTitle()}"
                next.isToday -> "Сегодня"
                else -> "Следующая · ${next.date.weekdayShort()}, ${next.date.shortTitle()}"
            }
            Card(brush = kindGradient(w.kind), padding = 20.dp, onClick = { onOpen(WorkoutRef(next.week, next.day)) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(label, Color.White)
                    Pill(w.kind.title, Color.White)
                }
                Text(w.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    "≈ ${minutesText(Program.durationSec(w))} · ≈ ${Program.kcal(w, state.currentWeight)} ккал",
                    color = Color.White.copy(alpha = 0.9f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    Program.distinctExercises(w).filter { it.category.name !in setOf("WARMUP", "STRETCH") }.take(4).forEach { ex ->
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.18f))) {
                            MoveFigure(ex.anim, Modifier.fillMaxSize().padding(4.dp), animate = false, color = Color.White)
                        }
                    }
                }
                Button(
                    onClick = { onStart(WorkoutRef(next.week, next.day)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A1A1A)),
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Text("  Начать", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (rawWeek >= 1) {
            Card(brush = HeroGradient, padding = 20.dp) {
                Text("Все тренировки программы выполнены 🎉", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Можно пройти последнюю неделю ещё раз или начать программу заново в настройках.", color = Color.White)
            }
        }

        DailyCard(state, week, today, onOpen, onStart)

        WeekStrip(state, week, today, onOpen)

        FoodTodayCard(rawWeek, today, onFood)

        WeightCard(state, onLogWeight)

        HabitsCard(state, today, phase.steps, onToggleHabit)

        val tip = Nutrition.tips[(today.toEpochDay() % Nutrition.tips.size).toInt()]
        Card(color = Surface1) {
            Text("СОВЕТ ДНЯ", style = Caption)
            Text(tip)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WeekStrip(state: AppState, week: Int, today: LocalDate, onOpen: (WorkoutRef) -> Unit) {
    val s = state.settings
    val done = state.doneKeys
    val weekStart = LocalDate.ofEpochDay(s.startEpochDay + (week - 1) * 7L)
    val days = s.trainingDays.sorted()
    Card(color = Surface1) {
        Text("НЕДЕЛЯ $week", style = Caption)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val dayIdx = days.indexOf(i + 1).takeIf { it in 0 until Program.DAYS }
                val isDone = dayIdx != null && Program.key(week, dayIdx) in done
                val isToday = date == today
                val kind = dayIdx?.let { Program.workout(week, it, s, state.swaps).kind }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = dayIdx != null) { dayIdx?.let { onOpen(WorkoutRef(week, it)) } }
                        .padding(4.dp),
                ) {
                    Text(WeekdayShort[i], fontSize = 12.sp, color = if (isToday) Accent else Muted)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> Success
                                    kind != null -> kindColor(kind).copy(alpha = 0.18f)
                                    else -> Color.Transparent
                                },
                            )
                            .border(if (isToday) 2.dp else 0.dp, if (isToday) Accent else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) {
                            Icon(Icons.Filled.Check, null, tint = Color(0xFF04150D), modifier = Modifier.size(20.dp))
                        } else {
                            Text("${date.dayOfMonth}", color = if (kind != null) kindColor(kind) else Muted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightCard(state: AppState, onLogWeight: () -> Unit) {
    val p = state.profile
    val cur = state.currentWeight
    val lost = p.startWeight - cur
    val total = (p.startWeight - p.goalWeight).coerceAtLeast(0.1)
    Card(color = Surface1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ВЕС", style = Caption)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(kg(cur), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" кг", color = Muted, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    if (lost > 0.05) "−${kg(lost)} кг с начала · до цели ${kg((cur - p.goalWeight).coerceAtLeast(0.0))} кг"
                    else "Цель ${kg(p.goalWeight)} кг",
                    color = if (lost > 0.05) Success else Muted, fontSize = 14.sp,
                )
            }
            Button(onClick = onLogWeight, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextMain)) {
                Text("Записать")
            }
        }
        LinearProgressIndicator(
            progress = { (lost / total).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Success,
            trackColor = Surface2,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun HabitsCard(state: AppState, today: LocalDate, steps: Int, onToggle: (String) -> Unit) {
    val checked = state.habits[today.toEpochDay()].orEmpty()
    val water = com.edward.calisthenics.data.Nutrition.plan(state.profile, state.currentWeight).waterL
    Card(color = Surface1) {
        Row {
            Text("ПРИВЫЧКИ НА СЕГОДНЯ", style = Caption, modifier = Modifier.weight(1f))
            Text("${checked.size}/${Nutrition.habits.size}", style = Caption)
        }
        Nutrition.habits.forEach { (id, title) ->
            val on = id in checked
            val label = when (id) {
                "steps" -> "$title: ${"%,d".format(Ru, steps)}"
                "water" -> "$title: ${kg(water)} л"
                else -> title
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggle(id) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (on) Success else Color.Transparent)
                        .border(2.dp, if (on) Success else Surface3, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Icon(Icons.Filled.Check, null, tint = Color(0xFF04150D), modifier = Modifier.size(16.dp))
                }
                Text(label, modifier = Modifier.padding(start = 12.dp), color = if (on) Muted else TextMain)
            }
        }
    }
}

@Composable
private fun FoodTodayCard(programWeek: Int, today: LocalDate, onFood: () -> Unit) {
    val day = com.edward.calisthenics.data.Food.day(programWeek, today.dayOfWeek.value)
    val (kcal, _) = com.edward.calisthenics.data.Food.dayTotals(day)
    Card(color = Surface1, onClick = onFood) {
        Row {
            Text("ПИТАНИЕ СЕГОДНЯ · ${com.edward.calisthenics.data.Food.menuTitle(programWeek).uppercase()}", style = Caption, modifier = Modifier.weight(1f))
            Text("≈ $kcal ккал", style = Caption)
        }
        day.ids().forEach { (meal, id) ->
            val r = com.edward.calisthenics.data.Food.recipe(id)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meal.title, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp).width(76.dp))
                Text(r.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (r.outside != null) Pill("кафе", Rest)
            }
        }
        com.edward.calisthenics.data.Food.prep(programWeek, today.dayOfWeek.value)?.let { Text(it, color = AccentSoft, fontSize = 14.sp) }
    }
}

/** Карточка ежедневной утренней статики. */
@Composable
private fun DailyCard(state: AppState, week: Int, today: LocalDate, onOpen: (WorkoutRef) -> Unit, onStart: (WorkoutRef) -> Unit) {
    val w = Program.daily(week, state.settings, state.swaps, today)
    val done = w.key in state.doneKeys
    val ref = WorkoutRef(daily = true)
    Card(color = Surface1, onClick = { onOpen(ref) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(kindColor(w.kind).copy(alpha = 0.16f))) {
                MoveFigure("side_plank", Modifier.fillMaxSize().padding(4.dp), animate = false, color = kindColor(w.kind))
            }
            Column(Modifier.weight(1f)) {
                Text("Каждый день", style = Caption)
                Text(w.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (done) "Сегодня сделано ✓" else "≈ ${minutesText(Program.durationSec(w))} · планки и удержания",
                    color = if (done) Success else Muted,
                )
            }
            if (done) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Success.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, "Сделано", tint = Success)
                }
            } else {
                IconButton(
                    onClick = { onStart(ref) },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(kindColor(w.kind)),
                ) { Icon(Icons.Filled.PlayArrow, "Начать", tint = Color.White) }
            }
        }
    }
}
