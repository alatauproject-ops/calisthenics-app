package com.edward.calisthenics.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.Feedback
import com.edward.calisthenics.data.ExerciseLog
import com.edward.calisthenics.data.Measure
import com.edward.calisthenics.data.Program
import com.edward.calisthenics.data.Step
import com.edward.calisthenics.data.Workout
import kotlinx.coroutines.delay

data class PlayerResult(
    val logs: List<ExerciseLog>,
    val startedAt: Long,
    val durationSec: Int,
    val kcal: Int,
    val completed: Boolean,
    val workDone: Int,
    val workTotal: Int,
    val totalReps: Int,
)

private enum class TimerPhase { READY, RUN }

private val SIDE_SWITCH = setOf("side_plank", "quad_stretch")

@Composable
fun PlayerScreen(
    workout: Workout,
    weightKg: Double,
    feedback: Feedback,
    onFinish: (PlayerResult) -> Unit,
    onExit: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val steps = remember(workout) { Program.steps(workout) }
    val works = remember(steps) { steps.filterIsInstance<Step.Work>() }
    val startedAt = remember { System.currentTimeMillis() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var index by remember { mutableIntStateOf(0) }
    val results = remember { mutableStateMapOf<Int, Int>() }

    var count by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(TimerPhase.READY) }
    var readyEnd by remember { mutableLongStateOf(0L) }
    var runAccum by remember { mutableLongStateOf(0L) }
    var runStart by remember { mutableStateOf<Long?>(null) }
    var restEnd by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var pausedLeft by remember { mutableLongStateOf(0L) }
    var lastBeep by remember { mutableIntStateOf(-1) }
    var halfSaid by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var showTech by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }

    val step = steps.getOrNull(index)

    fun nextWork(from: Int): Step.Work? = steps.drop(from + 1).firstOrNull { it is Step.Work } as? Step.Work

    fun finish(completedAll: Boolean) {
        if (finished) return
        finished = true
        val logs = linkedMapOf<String, MutableList<Int>>()
        var reps = 0
        var workSec = 0.0
        var kcal = 0.0
        for ((i, s) in steps.withIndex()) {
            if (s !is Step.Work) continue
            val v = results[i] ?: continue
            val ex = s.item.exercise
            logs.getOrPut(ex.id) { mutableListOf() } += v
            if (ex.measure == Measure.REPS) reps += v
            workSec += if (ex.measure == Measure.TIME) v.toDouble() else v * ex.secPerRep
            kcal += Program.kcalFor(ex, v, weightKg)
        }
        val duration = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
        kcal += 2.0 * weightKg * (duration - workSec).coerceAtLeast(0.0) / 3600.0
        val done = results.size
        feedback.done()
        feedback.say(if (completedAll) "Тренировка завершена. Отличная работа!" else "Тренировка сохранена")
        onFinish(
            PlayerResult(
                logs.map { (id, v) -> ExerciseLog(id, v) }, startedAt, duration, kcal.toInt(),
                completed = done >= works.size * 0.6, workDone = done, workTotal = works.size, totalReps = reps,
            ),
        )
    }

    fun enter(i: Int) {
        if (i > steps.lastIndex) {
            finish(true)
            return
        }
        index = i
        count = 0
        runAccum = 0
        runStart = null
        paused = false
        lastBeep = -1
        halfSaid = false
        val t = System.currentTimeMillis()
        when (val s = steps[i]) {
            is Step.Work -> {
                val ex = s.item.exercise
                val prev = steps.getOrNull(i - 1)
                val continuous = prev is Step.Work && prev.item.exercise.fixedVolume && ex.fixedVolume
                if (ex.measure == Measure.TIME) {
                    if (continuous) {
                        phase = TimerPhase.RUN
                        runStart = t
                        feedback.go()
                    } else {
                        phase = TimerPhase.READY
                        readyEnd = t + 3000
                    }
                }
                feedback.say("${ex.name}. ${itemText(s.item)}")
            }
            is Step.Rest -> {
                restEnd = t + s.seconds * 1000L
                val next = nextWork(i)
                feedback.say("Отдых ${s.seconds} секунд." + (next?.let { " Дальше: ${it.item.exercise.name}" } ?: ""))
            }
        }
    }

    LaunchedEffect(Unit) { enter(0) }

    fun record(value: Int) {
        results[index] = value
        feedback.tick()
        enter(index + 1)
    }

    fun togglePause() {
        val s = step ?: return
        val t = System.currentTimeMillis()
        if (s is Step.Rest) {
            if (!paused) {
                pausedLeft = restEnd - t
                paused = true
            } else {
                restEnd = t + pausedLeft
                paused = false
            }
            return
        }
        if (phase == TimerPhase.READY) {
            phase = TimerPhase.RUN
            runStart = t
            paused = false
            return
        }
        val rs = runStart
        if (rs != null) {
            runAccum += t - rs
            runStart = null
            paused = true
        } else {
            runStart = t
            paused = false
        }
    }

    // Таймеры: подготовка, работа на время и отдых.
    LaunchedEffect(now) {
        val s = step ?: return@LaunchedEffect
        if (finished || paused) return@LaunchedEffect
        when (s) {
            is Step.Rest -> {
                val left = ((restEnd - now + 999) / 1000).toInt()
                if (left <= 0) {
                    enter(index + 1)
                } else if (left <= 3 && left != lastBeep) {
                    lastBeep = left
                    feedback.beep()
                }
            }
            is Step.Work -> {
                if (s.item.exercise.measure != Measure.TIME) return@LaunchedEffect
                if (phase == TimerPhase.READY) {
                    val left = ((readyEnd - now + 999) / 1000).toInt()
                    if (left <= 0) {
                        phase = TimerPhase.RUN
                        runStart = now
                        lastBeep = -1
                        feedback.go()
                    } else if (left != lastBeep) {
                        lastBeep = left
                        feedback.beep()
                    }
                } else {
                    val elapsed = runAccum + (runStart?.let { now - it } ?: 0L)
                    val target = s.item.value * 1000L
                    val left = ((target - elapsed + 999) / 1000).toInt()
                    if (!halfSaid && s.item.exercise.id in SIDE_SWITCH && elapsed >= target / 2) {
                        halfSaid = true
                        feedback.go()
                        feedback.say("Смена стороны")
                    }
                    if (left <= 0) {
                        feedback.done()
                        record(s.item.value)
                    } else if (left <= 3 && left != lastBeep) {
                        lastBeep = left
                        feedback.beep()
                    }
                }
            }
        }
    }

    BackHandler { confirmExit = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Верхняя панель.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Filled.Close, "Выйти") }
            Column(Modifier.weight(1f)) {
                Text(workout.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val sub = when (step) {
                    is Step.Work -> step.block.title + if (step.block.rounds > 1) " · круг ${step.round} из ${step.block.rounds}" else ""
                    else -> "Отдых"
                }
                Text(sub, style = Caption)
            }
            Text(clock(((now - startedAt) / 1000).toInt()), color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
        }
        LinearProgressIndicator(
            progress = { results.size.toFloat() / works.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Accent,
            trackColor = Surface2,
            strokeCap = StrokeCap.Round,
        )

        when (step) {
            null -> Spacer(Modifier.weight(1f))
            is Step.Rest -> RestView(
                step = step,
                leftMs = if (paused) pausedLeft else restEnd - now,
                paused = paused,
                next = nextWork(index),
                workCount = works.size,
                onAdd = { if (paused) pausedLeft += 20_000 else restEnd += 20_000 },
                onSkip = { enter(index + 1) },
                onPause = { togglePause() },
            )
            is Step.Work -> {
                val item = step.item
                val ex = item.exercise
                // Анимация техники.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.95f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardGradient),
                ) {
                    MoveFigure(ex.anim, Modifier.fillMaxSize().padding(12.dp))
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(ex.category.title, categoryColor(ex.category))
                        Pill("${step.index + 1} / ${works.size}", Muted)
                    }
                    IconButton(onClick = { showTech = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Filled.Info, "Техника", tint = Muted)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(ex.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    itemText(item) + (ex.repNote?.let { " · $it" } ?: ""),
                    color = AccentSoft, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    if (ex.measure == Measure.REPS) {
                        RepRing(count, item.value) {
                            count++
                            feedback.tick()
                            if (count == item.value) feedback.beep()
                        }
                    } else {
                        val elapsed = runAccum + (runStart?.let { now - it } ?: 0L)
                        TimeRing(
                            ready = phase == TimerPhase.READY,
                            readyLeft = ((readyEnd - now + 999) / 1000).toInt().coerceAtLeast(1),
                            elapsedMs = elapsed,
                            targetSec = item.value,
                            paused = paused,
                            onTap = { togglePause() },
                        )
                    }
                }
                // Кнопки.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundButton(Icons.Filled.SkipPrevious, "Назад", enabled = index > 0) {
                        val prev = (index - 1 downTo 0).firstOrNull { steps[it] is Step.Work }
                        if (prev != null) {
                            results.remove(prev)
                            enter(prev)
                        }
                    }
                    if (ex.measure == Measure.REPS) {
                        FilledTonalButton(
                            onClick = { if (count > 0) count-- },
                            modifier = Modifier.height(58.dp),
                            shape = RoundedCornerShape(20.dp),
                        ) { Text("−1", fontSize = 18.sp) }
                        Button(
                            onClick = { record(if (count > 0) count else item.value) },
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color(0xFF04150D)),
                        ) {
                            Icon(Icons.Filled.Check, null)
                            Text(if (count > 0) "  Готово: $count" else "  Готово", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { togglePause() },
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (paused) Accent else Surface2, contentColor = TextMain),
                        ) {
                            val running = phase == TimerPhase.RUN && !paused
                            Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null)
                            Text(
                                when {
                                    phase == TimerPhase.READY -> "  Старт сейчас"
                                    running -> "  Пауза"
                                    else -> "  Продолжить"
                                },
                                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                val elapsed = runAccum + (runStart?.let { System.currentTimeMillis() - it } ?: 0L)
                                record(((elapsed + 500) / 1000).toInt().coerceAtLeast(1))
                            },
                            modifier = Modifier.height(58.dp),
                            shape = RoundedCornerShape(20.dp),
                        ) { Text("Готово") }
                    }
                    RoundButton(Icons.Filled.SkipNext, "Пропустить") { enter(index + 1) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showTech && step is Step.Work) {
        val ex = step.item.exercise
        AlertDialog(
            onDismissRequest = { showTech = false },
            title = { Text(ex.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ex.cues.forEach { Bullet(it, Success) }
                    if (ex.mistakes.isNotEmpty()) {
                        Text("Ошибки", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        ex.mistakes.forEach { Bullet(it, Danger) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTech = false }) { Text("Понятно") } },
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Закончить тренировку?") },
            text = { Text("Выполнено упражнений: ${results.size} из ${works.size}. Можно сохранить результат или выйти без сохранения.") },
            confirmButton = {
                Button(onClick = { confirmExit = false; if (results.isEmpty()) onExit() else finish(false) }) { Text("Сохранить") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmExit = false; onExit() }) { Text("Не сохранять", color = Danger) }
                    TextButton(onClick = { confirmExit = false }) { Text("Назад") }
                }
            },
        )
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Surface1)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (enabled) TextMain else Muted.copy(alpha = 0.4f))
    }
}

@Composable
private fun RepRing(count: Int, target: Int, onTap: () -> Unit) {
    val reached = count >= target
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ring(
            progress = count.toFloat() / target.coerceAtLeast(1),
            color = if (reached) Success else Accent,
            modifier = Modifier
                .fillMaxHeight(0.86f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
            stroke = 12.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = if (reached) Success else TextMain)
                Text("из $target", color = Muted, fontSize = 15.sp)
            }
        }
        Text("Нажимайте на круг на каждом повторе", style = Caption, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TimeRing(ready: Boolean, readyLeft: Int, elapsedMs: Long, targetSec: Int, paused: Boolean, onTap: () -> Unit) {
    val left = ((targetSec * 1000L - elapsedMs + 999) / 1000).toInt().coerceAtLeast(0)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ring(
            progress = if (ready) 0f else elapsedMs / (targetSec * 1000f),
            color = if (ready) Rest else Accent,
            modifier = Modifier
                .fillMaxHeight(0.86f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
            stroke = 12.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (ready) {
                    Text("Приготовьтесь", color = Rest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("$readyLeft", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                } else {
                    Text(clock(left), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = if (paused) Muted else TextMain)
                    Text(if (paused) "пауза" else "осталось", color = Muted, fontSize = 14.sp)
                }
            }
        }
        Text(if (paused) "Нажмите на круг, чтобы продолжить" else "Нажмите на круг для паузы", style = Caption, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RestView(
    step: Step.Rest,
    leftMs: Long,
    paused: Boolean,
    next: Step.Work?,
    workCount: Int,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
) {
    val left = ((leftMs + 999) / 1000).toInt().coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(step.title.uppercase(), color = Rest, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        Ring(
            progress = leftMs / (step.seconds * 1000f),
            color = Rest,
            modifier = Modifier.size(220.dp).clip(CircleShape).clickable(onClick = onPause),
            stroke = 12.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clock(left), fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = if (paused) Muted else TextMain)
                if (paused) Text("пауза", color = Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(18.dp), modifier = Modifier.height(50.dp)) { Text("+20 сек") }
            Button(
                onClick = onSkip,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rest, contentColor = Color(0xFF03202C)),
            ) { Text("Пропустить отдых", fontWeight = FontWeight.Bold) }
        }
    }
    if (next != null) {
        Card(color = Surface1, padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(18.dp)).background(Surface2)) {
                    MoveFigure(next.item.exercise.anim, Modifier.fillMaxSize().padding(6.dp))
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("ДАЛЬШЕ · ${next.index + 1} из $workCount", style = Caption)
                    Text(next.item.exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(itemText(next.item), color = AccentSoft, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
