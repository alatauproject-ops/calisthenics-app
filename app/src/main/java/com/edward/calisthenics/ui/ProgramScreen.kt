package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Catalog
import com.edward.calisthenics.data.Exercise
import com.edward.calisthenics.data.Measure
import com.edward.calisthenics.formatValue

@Composable
fun ProgramScreen(state: AppState, vm: AppViewModel) {
    var selected by remember { mutableStateOf<Exercise?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenTitle(
                "Программа",
                "Две тренировки на всё тело, A и B, чередуются. Когда цель выполнена во всех подходах, " +
                    "она растёт сама, а после максимума открывается следующая ступень. " +
                    "Нажмите ▶, чтобы потренировать упражнение отдельно.",
            )
        }
        item {
            SectionCard {
                Text("A: " + Catalog.workoutA.joinToString(", ") { Catalog.chain(it)!!.title }, fontSize = 14.sp)
                Text("B: " + Catalog.workoutB.joinToString(", ") { Catalog.chain(it)!!.title }, fontSize = 14.sp)
                Text(
                    "Подходов на упражнение: ${state.settings.level.sets} (${state.settings.level.title.lowercase()} уровень)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(Catalog.chains, key = { it.id }) { chain ->
            val p = vm.progressFor(state, chain.id)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${chain.emoji} ${chain.title}", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    val cur = chain.steps[p.step.coerceIn(0, chain.steps.lastIndex)]
                    OutlinedButton(onClick = { vm.adjustTarget(chain.id, if (cur.measure == Measure.HOLD) -5 else -1) }) {
                        Text("−")
                    }
                    Text(
                        formatValue(cur.measure, p.target),
                        Modifier.padding(horizontal = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                    )
                    OutlinedButton(onClick = { vm.adjustTarget(chain.id, if (cur.measure == Measure.HOLD) 5 else 1) }) {
                        Text("+")
                    }
                }
                chain.steps.forEach { ex ->
                    val current = ex.step == p.step
                    val passed = ex.step < p.step
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (current) Accent.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { selected = ex }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(
                                    when {
                                        passed -> Success
                                        current -> Accent
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (passed) "✓" else "${ex.step + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (passed || current) Color(0xFF111111) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(ex.name, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                            Text(
                                "${formatValue(ex.measure, ex.startTarget)} → ${formatValue(ex.measure, ex.goalTarget)} ${unit(ex.measure)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalIconButton(onClick = { vm.startFree(ex.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.PlayArrow, "Счётчик")
                        }
                    }
                }
            }
        }
    }

    selected?.let { ex ->
        val chain = Catalog.chain(ex.chainId)!!
        val isCurrent = vm.progressFor(state, chain.id).step == ex.step
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(ex.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ex.cues)
                    Text(
                        "Начальная цель: ${formatValue(ex.measure, ex.startTarget)} ${unit(ex.measure)}. " +
                            "Переход дальше: ${formatValue(ex.measure, ex.goalTarget)} ${unit(ex.measure)} во всех подходах.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.startFree(ex.id); selected = null }) { Text("Счётчик") }
            },
            dismissButton = {
                if (!isCurrent) {
                    TextButton(onClick = { vm.setStep(chain.id, ex.step); selected = null }) {
                        Text("Сделать текущей")
                    }
                } else {
                    TextButton(onClick = { selected = null }) { Text("OK") }
                }
            },
        )
    }
}
