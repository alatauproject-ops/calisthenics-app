package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.data.Session
import com.edward.calisthenics.formatValue
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(state: AppState, vm: AppViewModel) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var selected by remember { mutableStateOf(today) }
    var toDelete by remember { mutableStateOf<Session?>(null) }
    val byDay = state.sessionsByDay()
    val monthCount = state.sessions.count {
        val d = LocalDate.ofEpochDay(it.epochDay)
        d.year == month.year && d.month == month.month
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Календарь", "Тренировок в этом месяце: $monthCount")

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущий месяц")
                }
                Text(
                    month.monthTitle(),
                    Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующий месяц")
                }
            }
            Row(Modifier.fillMaxWidth()) {
                WeekdayShort.forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            val gridStart = month.weekStart()
            for (week in 0 until 6) {
                val rowStart = gridStart.plusWeeks(week.toLong())
                if (week > 0 && rowStart.month != month.month) break
                Row(Modifier.fillMaxWidth()) {
                    for (i in 0..6) {
                        val d = rowStart.plusDays(i.toLong())
                        DayCell(
                            date = d,
                            inMonth = d.month == month.month,
                            done = byDay[d.toEpochDay()].orEmpty().isNotEmpty(),
                            planned = state.settings.isTrainingDay(d) && !d.isBefore(today),
                            isToday = d == today,
                            isSelected = d == selected,
                            onClick = { selected = d },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(Success, filled = true, "выполнено")
                Legend(Accent, filled = false, "по плану")
            }
        }

        val daySessions = byDay[selected.toEpochDay()].orEmpty()
        Text(selected.longTitle(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (daySessions.isEmpty()) {
            Text(
                when {
                    state.settings.isTrainingDay(selected) && !selected.isBefore(today) -> "Запланирована тренировка."
                    state.settings.isTrainingDay(selected) -> "Тренировка по плану была пропущена."
                    else -> "День отдыха."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        daySessions.forEach { s -> SessionCard(s, onDelete = { toDelete = s }) }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить тренировку?") },
            text = { Text("${s.title} будет удалена из истории. Прогрессии не изменятся.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSession(s.id); toDelete = null }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    done: Boolean,
    planned: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(if (done) Success else Color.Transparent, CircleShape)
                .border(2.dp, if (planned && !done) Accent.copy(alpha = if (inMonth) 1f else 0.4f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = when {
                    done -> Color(0xFF05160B)
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isToday -> Accent
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun Legend(color: Color, filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(if (filled) color else Color.Transparent, CircleShape)
                .border(2.dp, color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SessionCard(s: Session, onDelete: (() -> Unit)? = null) {
    val time = java.time.Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "$time · ${durationText(s.durationSec)} · ${s.totalReps()} повт.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        s.entries.forEach { e ->
            val ex = Catalog.exercise(e.exerciseId)
            Row(Modifier.fillMaxWidth()) {
                Text(ex?.name ?: e.exerciseId, Modifier.weight(1f))
                Text(
                    e.sets.joinToString(" · ") { v -> ex?.let { formatValue(it.measure, v) } ?: "$v" },
                    color = if (e.sets.all { it >= e.target }) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
