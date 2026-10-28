package com.edward.calisthenics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Program
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ProgressScreen(state: AppState, vm: AppViewModel, onLogWeight: () -> Unit) {
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Прогресс", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        WeightCard(state, onLogWeight)
        WeightChart(state)

        val sessions = state.sessions
        val totalMin = sessions.sumOf { it.durationSec } / 60
        val totalKcal = sessions.sumOf { it.kcal }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${sessions.size}", "тренировок", Modifier.weight(1f))
            Stat("$totalMin", "минут", Modifier.weight(1f))
            Stat("$totalKcal", "ккал", Modifier.weight(1f), color = Accent)
        }

        MonthCalendar(state)

        if (state.weights.size > 1) {
            SectionTitle("Взвешивания")
            state.weights.sortedByDescending { it.epochDay }.take(10).forEach { w ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(LocalDate.ofEpochDay(w.epochDay).shortTitle(), modifier = Modifier.weight(1f), color = Muted)
                    Text("${kg(w.kg)} кг", fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { vm.deleteWeight(w.epochDay) }) { Icon(Icons.Filled.Delete, "Удалить", tint = Muted) }
                }
            }
        }

        SectionTitle("История")
        if (sessions.isEmpty()) Text("Здесь появятся выполненные тренировки.", color = Muted)
        sessions.sortedByDescending { it.id }.take(30).forEach { s ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${LocalDate.ofEpochDay(s.epochDay).shortTitle()} · ${minutesText(s.durationSec)} · ${s.kcal} ккал" +
                            if (!s.completed && s.key.startsWith("w")) " · частично" else "",
                        style = Caption,
                    )
                }
                IconButton(onClick = { confirmDelete = s.id }) { Icon(Icons.Filled.Delete, "Удалить", tint = Muted) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Удалить тренировку?") },
            text = { Text("Запись пропадёт из истории и прогресса программы.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(id); confirmDelete = null }) { Text("Удалить", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Отмена") } },
        )
    }
}

/** График веса: факт, план до цели за 12 недель и коридор 82–85. */
@Composable
private fun WeightChart(state: AppState) {
    val p = state.profile
    val start = state.settings.startEpochDay
    val end = start + Program.WEEKS * 7L
    val pts = state.weights.sortedBy { it.epochDay }
    val minDay = minOf(start, pts.firstOrNull()?.epochDay ?: start)
    val maxDay = maxOf(end, pts.lastOrNull()?.epochDay ?: end)
    val hi = maxOf(p.startWeight, pts.maxOfOrNull { it.kg } ?: p.startWeight) + 1
    val lo = minOf(p.goalWeight - 4, pts.minOfOrNull { it.kg } ?: p.goalWeight) - 1

    Card(color = Surface1) {
        Text("ГРАФИК ВЕСА", style = Caption)
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.7f)) {
            val w = size.width
            val h = size.height
            fun x(day: Long) = ((day - minDay).toFloat() / (maxDay - minDay).coerceAtLeast(1)) * w
            fun y(kg: Double) = (((hi - kg) / (hi - lo)) * h).toFloat()

            // Коридор цели.
            drawRect(
                Success.copy(alpha = 0.12f),
                topLeft = Offset(0f, y(p.goalWeight)),
                size = androidx.compose.ui.geometry.Size(w, y(p.goalWeight - 3) - y(p.goalWeight)),
            )
            // Сетка по 5 кг.
            var g = (lo / 5).toInt() * 5.0
            while (g <= hi) {
                if (g >= lo) drawLine(Surface2, Offset(0f, y(g)), Offset(w, y(g)), strokeWidth = 1.dp.toPx())
                g += 5
            }
            // План.
            drawLine(
                Muted.copy(alpha = 0.6f), Offset(x(start), y(p.startWeight)), Offset(x(end), y(p.goalWeight)),
                strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
            // Факт.
            if (pts.isNotEmpty()) {
                val path = Path()
                pts.forEachIndexed { i, e ->
                    if (i == 0) path.moveTo(x(e.epochDay), y(e.kg)) else path.lineTo(x(e.epochDay), y(e.kg))
                }
                drawPath(path, Brush.horizontalGradient(listOf(Accent, Success)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                pts.forEach { drawCircle(Accent, 4.dp.toPx(), Offset(x(it.epochDay), y(it.kg))) }
            }
            // Сегодня.
            val today = LocalDate.now().toEpochDay()
            if (today in minDay..maxDay) {
                drawLine(Accent.copy(alpha = 0.4f), Offset(x(today), 0f), Offset(x(today), h), strokeWidth = 1.dp.toPx())
            }
        }
        Row {
            Text("${kg(hi)} кг", style = Caption, modifier = Modifier.weight(1f))
            Text("пунктир: план · зелёное: цель ${kg(p.goalWeight - 3)}–${kg(p.goalWeight)}", style = Caption)
        }
    }
}

@Composable
private fun MonthCalendar(state: AppState) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    val s = state.settings
    val byDay = state.sessions.groupBy { it.epochDay }
    val today = LocalDate.now()
    val planned = buildSet {
        for (wk in 1..Program.WEEKS) for (d in 0 until Program.DAYS) add(Program.dateOf(s, wk, d).toEpochDay())
    }

    Card(color = Surface1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад") }
            Text(ym.atDay(1).monthTitle(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            IconButton(onClick = { month = ym.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд") }
        }
        Row(Modifier.fillMaxWidth()) {
            WeekdayShort.forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = Caption) }
        }
        val first = ym.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val cells = offset + ym.lengthOfMonth()
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..ym.lengthOfMonth()) {
                            val date = ym.atDay(dayNum)
                            val e = date.toEpochDay()
                            val done = byDay[e].orEmpty().isNotEmpty()
                            val plan = e in planned
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            done -> Success
                                            plan && date.isBefore(today) -> Danger.copy(alpha = 0.18f)
                                            plan -> Accent.copy(alpha = 0.18f)
                                            else -> Color.Transparent
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$dayNum",
                                    fontSize = 13.sp,
                                    fontWeight = if (date == today) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = when {
                                        done -> Color(0xFF04150D)
                                        date == today -> Accent
                                        else -> TextMain
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend(Success, "сделано")
            Legend(Accent.copy(alpha = 0.5f), "по плану")
            Legend(Danger.copy(alpha = 0.5f), "пропущено")
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(" $text", style = Caption)
    }
}

@Composable
fun WeightDialog(initial: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(kg(initial).replace(',', '.')) }
    val value = text.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вес сегодня") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Лучше утром натощак, после туалета.", style = Caption)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { value?.let { text = kg(it - 0.1).replace(',', '.') } }) { Text("−0,1") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(6) },
                        singleLine = true,
                        suffix = { Text("кг") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = { value?.let { text = kg(it + 0.1).replace(',', '.') } }) { Text("+0,1") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { value?.let { if (it in 30.0..300.0) onSave(it) } }, enabled = value != null && value in 30.0..300.0) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
