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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.WorkoutRef
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Program
import java.time.LocalDate

@Composable
fun ProgramScreen(state: AppState, onOpen: (WorkoutRef) -> Unit) {
    val s = state.settings
    val today = LocalDate.now()
    val current = Program.weekOf(s, today).coerceIn(1, Program.WEEKS)
    val done = state.doneKeys
    var expanded by rememberSaveable { mutableStateOf(current) }
    val doneCount = done.count { it.startsWith("w") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Программа", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "12 недель, 4 тренировки в неделю. Выполнено $doneCount из ${Program.WEEKS * Program.DAYS}.",
                color = Muted,
            )
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { doneCount / (Program.WEEKS * Program.DAYS).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Success,
                trackColor = Surface2,
            )
        }
        Program.phases.forEach { phase ->
            item {
                Card(brush = CardGradient, modifier = Modifier.padding(top = 8.dp)) {
                    Text("НЕДЕЛИ ${phase.weeks.first}–${phase.weeks.last}", style = Caption)
                    Text("Фаза «${phase.title}»", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentSoft)
                    Text(phase.goal, color = Muted)
                    Text("Шаги каждый день: ${"%,d".format(Ru, phase.steps)}", fontWeight = FontWeight.SemiBold)
                }
            }
            items(phase.weeks.toList()) { week ->
                val weekDone = (0 until Program.DAYS).count { Program.key(week, it) in done }
                val open = expanded == week
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface1),
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable { expanded = if (open) 0 else week }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(
                                when {
                                    weekDone == Program.DAYS -> Success
                                    week == current -> Accent
                                    else -> Surface2
                                },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (weekDone == Program.DAYS) Icon(Icons.Filled.Check, null, tint = Color(0xFF04150D))
                            else Text("$week", fontWeight = FontWeight.Bold, color = if (week == current) Color.White else TextMain)
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("Неделя $week" + if (week == current) " · сейчас" else "", fontWeight = FontWeight.SemiBold)
                            val d0 = Program.dateOf(s, week, 0)
                            Text("с ${d0.minusDays((d0.dayOfWeek.value - 1).toLong()).shortTitle()} · выполнено $weekDone/${Program.DAYS}", style = Caption)
                        }
                        Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Muted)
                    }
                    if (open) {
                        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (day in 0 until Program.DAYS) {
                                val w = Program.workout(week, day, s, state.swaps)
                                val date = Program.dateOf(s, week, day)
                                val isDone = Program.key(week, day) in done
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Surface2)
                                        .clickable { onOpen(WorkoutRef(week, day)) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(6.dp, 44.dp).clip(RoundedCornerShape(3.dp)).background(kindColor(w.kind)))
                                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                        Text(w.title, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${date.weekdayShort()}, ${date.shortTitle()} · ≈ ${minutesText(Program.durationSec(w))} · ${w.kind.title}",
                                            style = Caption,
                                        )
                                    }
                                    if (isDone) {
                                        Box(Modifier.size(28.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Check, null, tint = Color(0xFF04150D), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
