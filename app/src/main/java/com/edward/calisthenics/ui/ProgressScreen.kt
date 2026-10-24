package com.edward.calisthenics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.formatValue
import java.time.LocalDate

@Composable
fun ProgressScreen(state: AppState, vm: AppViewModel) {
    val today = LocalDate.now()
    val last30 = state.sessions.count { it.epochDay > today.toEpochDay() - 30 }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Прогресс")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${state.sessions.size}", "всего тренировок", Modifier.weight(1f))
            StatTile("$last30", "за 30 дней", Modifier.weight(1f))
            StatTile("${state.weekStreak()}", "недель подряд", Modifier.weight(1f))
        }

        WeeklyChart(state, today)

        SectionCard {
            Text("Уровни прогрессий", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Catalog.chains.forEach { chain ->
                val p = vm.progressFor(state, chain.id)
                val ex = chain.steps[p.step.coerceIn(0, chain.steps.lastIndex)]
                val span = (ex.goalTarget - ex.startTarget).coerceAtLeast(1)
                val frac = ((p.target - ex.startTarget).toFloat() / span).coerceIn(0f, 1f)
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("${chain.emoji} ${ex.name}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${p.step + 1}/${chain.steps.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        "Цель ${formatValue(ex.measure, p.target)} → до новой ступени нужно ${formatValue(ex.measure, ex.goalTarget)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val bests = state.personalBests()
        if (bests.isNotEmpty()) {
            SectionCard {
                Text("Личные рекорды (лучший подход)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                bests.forEach { (id, best) ->
                    val ex = Catalog.exercise(id) ?: return@forEach
                    Row(Modifier.fillMaxWidth()) {
                        Text(ex.name, Modifier.weight(1f))
                        Text(
                            "${formatValue(ex.measure, best)} ${unit(ex.measure)}",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChart(state: AppState, today: LocalDate) {
    val weeks = 8
    val thisWeek = today.weekStart()
    val starts = (weeks - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
    val totals = starts.map { ws ->
        state.sessions.filter { LocalDate.ofEpochDay(it.epochDay).weekStart() == ws }.sumOf { it.totalReps() }
    }
    val max = (totals.maxOrNull() ?: 0).coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant

    SectionCard {
        Text("Повторения по неделям", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Эта неделя: ${totals.last()} · максимум: ${totals.maxOrNull() ?: 0}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth()) {
            totals.forEach { v ->
                Text(
                    if (v > 0) "$v" else "",
                    Modifier.weight(1f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / weeks
            val barW = slot * 0.6f
            totals.forEachIndexed { i, v ->
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(8f, 8f))
                val h = size.height * v / max
                if (h > 0f) {
                    drawRoundRect(
                        if (i == weeks - 1) Accent else Success,
                        Offset(x, size.height - h),
                        Size(barW, h),
                        CornerRadius(8f, 8f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            starts.forEach { d ->
                Text(
                    "${d.dayOfMonth}.${"%02d".format(d.monthValue)}",
                    Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
