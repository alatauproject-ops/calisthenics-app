package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.TITLE_A
import com.edward.calisthenics.TITLE_B
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.formatValue
import java.time.LocalDate

@Composable
fun TodayScreen(
    state: AppState,
    vm: AppViewModel,
    onQuickCounter: () -> Unit,
) {
    val today = LocalDate.now()
    val byDay = state.sessionsByDay()
    val doneToday = byDay[today.toEpochDay()].orEmpty().isNotEmpty()
    val trainingDay = state.settings.isTrainingDay(today)
    val nextTitle = vm.nextWorkoutTitle(state)
    val otherTitle = if (nextTitle == TITLE_A) TITLE_B else TITLE_A
    val plan = vm.planFor(state, nextTitle)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(today.longTitle(), "Уровень: ${state.settings.level.title}")

        WeekStrip(state, today)

        SectionCard {
            val headline = when {
                doneToday -> "Сегодня тренировка выполнена ✓"
                trainingDay -> "Сегодня день тренировки"
                else -> "Сегодня по плану отдых"
            }
            Text(headline, color = if (doneToday) Success else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(nextTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            plan.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.exercise.name, Modifier.weight(1f))
                    Text(
                        "${item.sets} × ${formatValue(item.exercise.measure, item.target)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.startPlanned(nextTitle) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Text("  Начать тренировку", fontSize = 17.sp)
            }
            OutlinedButton(onClick = { vm.startPlanned(otherTitle) }, modifier = Modifier.fillMaxWidth()) {
                Text("Вместо этого: $otherTitle")
            }
        }

        SectionCard {
            Text("Быстрый счётчик", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Считайте повторения или время удержания для любого упражнения без плана.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onQuickCounter, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать упражнение")
            }
        }

        val weekSessions = state.sessions.filter { LocalDate.ofEpochDay(it.epochDay).weekStart() == today.weekStart() }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${weekSessions.size}/${state.settings.trainingDays.size}", "на этой неделе", Modifier.weight(1f))
            StatTile("${weekSessions.sumOf { it.totalReps() }}", "повторений за неделю", Modifier.weight(1f))
            StatTile("${state.weekStreak()}", "недель подряд", Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeekStrip(state: AppState, today: LocalDate) {
    val start = today.weekStart()
    val byDay = state.sessionsByDay()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            val done = byDay[d.toEpochDay()].orEmpty().isNotEmpty()
            val planned = state.settings.isTrainingDay(d)
            val isToday = d == today
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(WeekdayShort[i], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(38.dp)
                        .background(
                            when {
                                done -> Success
                                isToday -> MaterialTheme.colorScheme.surfaceVariant
                                else -> Color.Transparent
                            },
                            CircleShape,
                        )
                        .border(
                            2.dp,
                            if (planned && !done) Accent else Color.Transparent,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${d.dayOfMonth}",
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (done) Color(0xFF05160B) else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
