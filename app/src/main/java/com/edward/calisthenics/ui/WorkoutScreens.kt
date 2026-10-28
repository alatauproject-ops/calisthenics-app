package com.edward.calisthenics.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Exercise
import com.edward.calisthenics.data.Exercises
import com.edward.calisthenics.data.Item
import com.edward.calisthenics.data.Kind
import com.edward.calisthenics.data.Program
import com.edward.calisthenics.data.Workout

@Composable
fun WorkoutPreviewScreen(
    state: AppState,
    workout: Workout,
    isDone: Boolean,
    dateLabel: String?,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onExercise: (String) -> Unit,
    onSwap: (originalId: String, newId: String?) -> Unit,
    onToggleDone: ((Boolean) -> Unit)?,
) {
    var swapFor by remember { mutableStateOf<Item?>(null) }
    val duration = Program.durationSec(workout)
    val kcal = Program.kcal(workout, state.currentWeight)

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(if (workout.week > 0) "Неделя ${workout.week} · день ${workout.day + 1}" else "Упражнение", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(brush = kindGradient(workout.kind), padding = 20.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(workout.kind.title, Color.White, filled = false)
                    if (dateLabel != null) Pill(dateLabel, Color.White)
                    if (isDone) Pill("Выполнено ✓", Color.White)
                }
                Text(workout.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(workout.note, color = Color.White.copy(alpha = 0.9f))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 4.dp)) {
                    HeroStat("≈ ${minutesText(duration)}", "время")
                    HeroStat("≈ $kcal", "ккал")
                    HeroStat("${Program.distinctExercises(workout).size}", "упражнений")
                }
            }

            workout.blocks.forEach { block ->
                val meta = buildList {
                    if (block.rounds > 1) add("${block.rounds} ${roundsWord(block.rounds)}")
                    if (block.restItem > 0) add("отдых ${block.restItem} с")
                    if (block.restRound > 0) add("между кругами ${block.restRound} с")
                }.joinToString(" · ")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(block.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (meta.isNotEmpty()) Text(meta, style = Caption)
                    }
                    block.items.forEach { item ->
                        ExerciseRow(
                            item = item,
                            swapped = item.exercise.id != item.originalId,
                            canSwap = workout.kind != Kind.SINGLE && swapOptions(item, state).size > 1,
                            onClick = { onExercise(item.exercise.id) },
                            onSwap = { swapFor = item },
                        )
                    }
                }
            }

            if (onToggleDone != null) {
                TextButton(onClick = { onToggleDone(!isDone) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isDone) "Снять отметку о выполнении" else "Уже сделал без телефона: отметить выполненной", color = Muted)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(if (isDone) "  Пройти ещё раз" else "  Начать тренировку", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    swapFor?.let { item ->
        val options = swapOptions(item, state)
        AlertDialog(
            onDismissRequest = { swapFor = null },
            title = { Text("Заменить упражнение") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Замена запомнится для всей программы.", style = Caption)
                    options.forEach { (ex, label) ->
                        val selected = ex.id == item.exercise.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Accent.copy(alpha = 0.16f) else Surface2)
                                .clickable {
                                    onSwap(item.originalId, ex.id)
                                    swapFor = null
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Thumb(ex.anim, size = 52.dp)
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(ex.name, fontWeight = FontWeight.SemiBold)
                                Text(label, style = Caption)
                            }
                            if (selected) Icon(Icons.Filled.Check, null, tint = Accent)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { swapFor = null }) { Text("Закрыть") } },
        )
    }
}

private fun roundsWord(n: Int) = when {
    n % 10 == 1 && n % 100 != 11 -> "круг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "круга"
    else -> "кругов"
}

/** Варианты замены: исходное упражнение программы, полегче и посложнее. */
fun swapOptions(item: Item, state: AppState): List<Pair<Exercise, String>> {
    val s = state.settings
    val out = linkedMapOf<String, Pair<Exercise, String>>()
    val original = Exercises.available(item.originalId, s)
    out[original.id] = original to "По программе"
    val cur = item.exercise
    out.putIfAbsent(cur.id, cur to "Сейчас")
    fun walk(start: Exercise, next: (Exercise) -> String?, label: String) {
        var e: Exercise? = start
        repeat(3) {
            val id = e?.let(next) ?: return
            val ex = Exercises.find(id) ?: return
            if (Exercises.usable(ex, s) && ex.measure == cur.measure) out.putIfAbsent(ex.id, ex to label)
            e = ex
        }
    }
    walk(original, { it.easier }, "Полегче")
    walk(original, { it.harder }, "Посложнее")
    return out.values.toList()
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun ExerciseRow(item: Item, swapped: Boolean, canSwap: Boolean, onClick: () -> Unit, onSwap: () -> Unit) {
    val ex = item.exercise
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(ex.anim, size = 68.dp, animate = true)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(itemText(item) + (ex.repNote?.let { " · $it" } ?: ""), color = AccentSoft, fontSize = 14.sp)
            if (swapped) Text("замена", style = Caption)
        }
        if (canSwap) {
            IconButton(onClick = onSwap) { Icon(Icons.Filled.SwapVert, "Заменить", tint = Muted) }
        }
    }
}

@Composable
fun FinishScreen(
    title: String,
    result: PlayerResult,
    streak: Int,
    onLogWeight: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(if (result.completed) HeroGradient else SeaGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
        Text(
            if (result.completed) "Тренировка выполнена!" else "Результат сохранён",
            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        )
        Text(title, color = Muted, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(minutesText(result.durationSec), "время", Modifier.weight(1f))
            Stat("${result.kcal}", "ккал (оценка)", Modifier.weight(1f), color = Accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${result.workDone}/${result.workTotal}", "упражнений", Modifier.weight(1f))
            Stat("${result.totalReps}", "повторов", Modifier.weight(1f))
        }
        if (streak > 1) {
            Card(color = Surface1) {
                Text("🔥 $streak тренировок подряд по плану", fontWeight = FontWeight.Bold)
                Text("Не выпадайте из графика: регулярность важнее рекордов.", color = Muted)
            }
        }
        OutlinedButton(onClick = onLogWeight, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
            Text("Записать вес")
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Готово", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}
