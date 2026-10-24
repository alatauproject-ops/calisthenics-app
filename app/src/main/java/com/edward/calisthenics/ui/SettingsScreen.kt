package com.edward.calisthenics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Level
import com.edward.calisthenics.formatSeconds

@Composable
fun SettingsScreen(state: AppState, vm: AppViewModel) {
    val s = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Настройки")

        SectionCard {
            Text("Уровень", fontWeight = FontWeight.Bold)
            LevelPicker(s.level) { lvl -> vm.updateSettings { it.copy(level = lvl) } }
            Text(
                "Уровень задаёт число подходов. Чтобы начать прогрессии с ступеней этого уровня, сбросьте их ниже.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Дни тренировок", fontWeight = FontWeight.Bold)
            DaysPicker(s.trainingDays) { days -> vm.updateSettings { it.copy(trainingDays = days) } }
        }

        SectionCard {
            Text("Отдых между подходами: ${formatSeconds(s.restSec)}", fontWeight = FontWeight.Bold)
            ChoiceRow(listOf(45, 60, 90, 120, 180), s.restSec, { formatSeconds(it) }) { v ->
                vm.updateSettings { it.copy(restSec = v) }
            }
        }

        SectionCard {
            ToggleRow("Звуковые сигналы", s.sound) { v -> vm.updateSettings { it.copy(sound = v) } }
            ToggleRow("Вибрация", s.vibration) { v -> vm.updateSettings { it.copy(vibration = v) } }
        }

        SectionCard {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить прогрессии под уровень")
            }
            OutlinedButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Удалить все данные", color = MaterialTheme.colorScheme.error)
            }
        }

        Text(
            "Калистеника 1.0 · все данные хранятся только на этом телефоне.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить прогрессии?") },
            text = { Text("Ступени и цели вернутся к стартовым для уровня «${s.level.title}». История тренировок сохранится.") },
            confirmButton = { TextButton(onClick = { vm.resetProgressions(); confirmReset = false }) { Text("Сбросить") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Удалить все данные?") },
            text = { Text("История, прогрессии и настройки будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAll(); confirmWipe = false }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Отмена") } },
        )
    }
}

@Composable
fun LevelPicker(level: Level, onPick: (Level) -> Unit) {
    ChoiceRow(Level.entries, level, { it.title }, onPick)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaysPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WeekdayShort.forEachIndexed { i, name ->
            val d = i + 1
            FilterChip(
                selected = d in days,
                onClick = {
                    val next = if (d in days) days - d else days + d
                    if (next.isNotEmpty()) onChange(next)
                },
                label = { Text(name) },
            )
        }
    }
    Text(
        "Выбрано: ${days.size} в неделю. Для начала хватит 3 дней с отдыхом между ними.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o)) })
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
