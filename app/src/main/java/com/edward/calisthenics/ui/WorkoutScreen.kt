package com.edward.calisthenics.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.Feedback
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.data.ExerciseLog
import com.edward.calisthenics.data.Measure
import com.edward.calisthenics.data.WorkoutPlan
import com.edward.calisthenics.formatSeconds
import com.edward.calisthenics.formatValue
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(
    plan: WorkoutPlan,
    restSec: Int,
    feedback: Feedback,
    onFinish: (logs: List<ExerciseLog>, startedAt: Long, durationSec: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val startedAt = remember { System.currentTimeMillis() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }

    val logs = remember { plan.items.map { mutableStateListOf<Int>() } }
    var index by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }

    // Таймер удержания: накопленное время + момент запуска.
    var holdAccumMs by remember { mutableLongStateOf(0L) }
    var holdStart by remember { mutableStateOf<Long?>(null) }
    var holdTargetSignaled by remember { mutableStateOf(false) }

    // Отдых: момент окончания.
    var restEnd by remember { mutableStateOf<Long?>(null) }
    var lastBeepSec by remember { mutableIntStateOf(-1) }

    var confirmExit by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }

    val item = plan.items[index]
    val ex = item.exercise
    val sets = logs[index]
    val chain = Catalog.chain(ex.chainId)
    val unlimited = item.sets == 0
    val exerciseDone = !unlimited && sets.size >= item.sets
    val allDone = !unlimited && plan.items.indices.all { logs[it].size >= plan.items[it].sets }

    val holdSec = ((holdAccumMs + (holdStart?.let { now - it } ?: 0L)) / 1000).toInt()
    val restLeft = restEnd?.let { ((it - now + 999) / 1000).toInt() }

    fun finish() {
        holdStart = null
        val result = plan.items.mapIndexed { i, it -> ExerciseLog(it.exercise.id, it.target, logs[i].toList()) }
        onFinish(result, startedAt, ((System.currentTimeMillis() - startedAt) / 1000).toInt())
    }

    fun resetCounter() {
        count = 0
        holdAccumMs = 0
        holdStart = null
        holdTargetSignaled = false
    }

    fun goTo(i: Int) {
        index = i.coerceIn(0, plan.items.lastIndex)
        resetCounter()
    }

    fun logSet() {
        val value = if (ex.measure == Measure.HOLD) holdSec else count
        if (value <= 0) return
        sets.add(value)
        resetCounter()
        feedback.tick()
        val doneNow = !unlimited && sets.size >= item.sets
        val everything = !unlimited && plan.items.indices.all { logs[it].size >= plan.items[it].sets }
        if (everything) {
            confirmFinish = true
            return
        }
        if (doneNow) {
            // Следующее невыполненное упражнение.
            val next = (1..plan.items.size).map { (index + it) % plan.items.size }
                .firstOrNull { logs[it].size < plan.items[it].sets }
            if (next != null) index = next
        }
        restEnd = System.currentTimeMillis() + restSec * 1000L
        lastBeepSec = -1
    }

    // Сигналы отдыха: короткие гудки в последние 3 секунды и длинный в конце.
    LaunchedEffect(restLeft) {
        val left = restLeft ?: return@LaunchedEffect
        if (left <= 0) {
            feedback.done()
            restEnd = null
        } else if (left <= 3 && left != lastBeepSec) {
            lastBeepSec = left
            feedback.beep()
        }
    }
    // Сигнал, когда удержание достигло цели.
    LaunchedEffect(holdSec) {
        if (holdStart != null && !holdTargetSignaled && holdSec >= item.target) {
            holdTargetSignaled = true
            feedback.done()
        }
    }

    BackHandler { confirmExit = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Верхняя панель.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Filled.Close, "Выйти") }
            Column(Modifier.weight(1f)) {
                Text(plan.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "⏱ ${formatClock(((now - startedAt) / 1000).toInt())}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            TextButton(onClick = { confirmFinish = true }) { Text("Завершить") }
        }

        if (plan.items.size > 1) {
            val doneSets = logs.sumOf { it.size }
            val totalSets = plan.items.sumOf { it.sets }
            LinearProgressIndicator(
                progress = { if (totalSets == 0) 0f else doneSets.toFloat() / totalSets },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                strokeCap = StrokeCap.Round,
            )
        }

        // Упражнение.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (plan.items.size > 1) {
                IconButton(onClick = { goTo(index - 1) }, enabled = index > 0 && restLeft == null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад")
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (plan.items.size > 1) {
                    Text(
                        "Упражнение ${index + 1} из ${plan.items.size}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(ex.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (chain != null) {
                    Text(
                        "${chain.emoji} ${chain.title} · ступень ${ex.step + 1} из ${chain.steps.size}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (plan.items.size > 1) {
                IconButton(onClick = { goTo(index + 1) }, enabled = index < plan.items.lastIndex && restLeft == null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд")
                }
            }
        }
        Text(
            ex.cues,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        // Подходы.
        Spacer(Modifier.height(10.dp))
        Text(
            if (unlimited) "Цель на подход: ${formatValue(ex.measure, item.target)}"
            else "Цель: ${item.sets} × ${formatValue(ex.measure, item.target)} ${unit(ex.measure)}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        SetsRow(sets, if (unlimited) sets.size + 1 else item.sets, item.target, ex.measure)

        // Центр: отдых, счётчик или таймер.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                restLeft != null -> RestPanel(
                    left = restLeft,
                    total = restSec,
                    nextName = ex.name,
                    onAdd = { restEnd = (restEnd ?: now) + 15_000 },
                    onSkip = { restEnd = null },
                )
                exerciseDone -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", fontSize = 64.sp, color = Success)
                    Text("Упражнение выполнено", fontSize = 18.sp)
                }
                ex.measure == Measure.REPS -> RepCounter(
                    count = count,
                    target = item.target,
                    onTap = {
                        count++
                        feedback.tick()
                        if (count == item.target) feedback.beep()
                    },
                )
                else -> HoldTimer(
                    seconds = holdSec,
                    target = item.target,
                    running = holdStart != null,
                    onToggle = {
                        val start = holdStart
                        if (start == null) {
                            holdStart = System.currentTimeMillis()
                            feedback.beep()
                        } else {
                            holdAccumMs += System.currentTimeMillis() - start
                            holdStart = null
                        }
                    },
                )
            }
        }

        // Нижние кнопки.
        if (restLeft == null && !exerciseDone) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ex.measure == Measure.REPS) {
                    OutlinedButton(
                        onClick = { if (count > 0) count-- },
                        modifier = Modifier.height(56.dp),
                    ) { Text("−1", fontSize = 18.sp) }
                } else {
                    OutlinedButton(onClick = { resetCounter() }, modifier = Modifier.height(56.dp)) { Text("Сброс") }
                }
                val value = if (ex.measure == Measure.HOLD) holdSec else count
                Button(
                    onClick = { logSet() },
                    enabled = value > 0,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = MaterialTheme.colorScheme.onSecondary),
                ) {
                    Text(
                        "Подход ${sets.size + 1} готов: ${formatValue(ex.measure, value)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else if (exerciseDone && restLeft == null) {
            Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(if (allDone) "Завершить тренировку" else "Завершить досрочно", fontSize = 17.sp)
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Выйти из тренировки?") },
            text = { Text("Можно сохранить уже выполненные подходы или выйти без сохранения.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; finish() }) { Text("Сохранить") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmExit = false; onCancel() }) {
                        Text("Не сохранять", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirmExit = false }) { Text("Продолжить") }
                }
            },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(if (allDone) "Отличная работа! 💪" else "Завершить тренировку?") },
            text = {
                val total = logs.sumOf { it.size }
                Text(
                    if (allDone) "Все подходы выполнены. Сохраняем результат?"
                    else "Выполнено подходов: $total. Невыполненные упражнения не повысят цель.",
                )
            },
            confirmButton = { Button(onClick = { confirmFinish = false; finish() }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Назад") } },
        )
    }
}

@Composable
private fun SetsRow(sets: List<Int>, slots: Int, target: Int, measure: Measure) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        for (i in 0 until slots.coerceAtMost(8)) {
            val v = sets.getOrNull(i)
            val current = i == sets.size
            Box(
                Modifier
                    .width(54.dp)
                    .height(40.dp)
                    .background(
                        when {
                            v == null -> MaterialTheme.colorScheme.surface
                            v >= target -> Success.copy(alpha = 0.25f)
                            else -> Accent.copy(alpha = 0.22f)
                        },
                        RoundedCornerShape(10.dp),
                    )
                    .border(
                        if (current) 2.dp else 0.dp,
                        if (current) Accent else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (v == null) "${i + 1}" else formatValue(measure, v),
                    fontWeight = if (v != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (v == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RepCounter(count: Int, target: Int, onTap: () -> Unit) {
    val reached = count >= target
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .background(if (reached) Success.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface, CircleShape)
                .border(4.dp, if (reached) Success else Accent, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontSize = 110.sp, fontWeight = FontWeight.Bold)
                Text("из $target", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Нажимайте на круг на каждом повторе", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HoldTimer(seconds: Int, target: Int, running: Boolean, onToggle: () -> Unit) {
    val progress = (seconds.toFloat() / target).coerceIn(0f, 1f)
    val reached = seconds >= target
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val d = size.minDimension - stroke
                val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
                drawArc(
                    if (reached) Success else Accent, -90f, 360f * progress, false, topLeft, Size(d, d),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(seconds), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text("цель ${formatSeconds(target)}", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (running) "Нажмите, чтобы остановить" else "Нажмите на круг, чтобы запустить",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RestPanel(left: Int, total: Int, nextName: String?, onAdd: () -> Unit, onSkip: () -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Отдых", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val d = size.minDimension - stroke
                val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
                drawArc(
                    Success, -90f, 360f * (left.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f), false,
                    topLeft, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(formatClock(left), fontSize = 64.sp, fontWeight = FontWeight.Bold)
        }
        if (nextName != null) {
            Spacer(Modifier.height(8.dp))
            Text("Дальше: $nextName", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onAdd) { Text("+15 сек") }
            Button(onClick = onSkip) { Text("Пропустить") }
        }
    }
}

fun formatClock(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
