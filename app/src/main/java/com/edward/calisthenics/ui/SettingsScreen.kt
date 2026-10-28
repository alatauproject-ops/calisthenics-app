package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.edward.calisthenics.AppViewModel
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Level
import com.edward.calisthenics.data.Profile
import com.edward.calisthenics.data.Settings
import java.time.LocalDate

@Composable
fun SettingsScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    val s = state.settings
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar("Настройки", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileEditor(state.profile) { p -> vm.updateProfile { p } }
            DaysEditor(s.trainingDays) { days -> vm.updateSettings { it.copy(trainingDays = days) } }
            StartEditor(s.startEpochDay) { e -> vm.updateSettings { it.copy(startEpochDay = e) } }
            EquipmentEditor(s) { ns -> vm.updateSettings { ns } }
            Card(color = Surface1) {
                Text("СЛОЖНОСТЬ", style = Caption)
                Text("Меняет число повторов и время упражнений во всей программе.", color = Muted, fontSize = 14.sp)
                Segmented(Level.entries.map { it.title }, s.level.ordinal) { i -> vm.updateSettings { it.copy(level = Level.entries[i]) } }
            }
            Card(color = Surface1) {
                Text("ВО ВРЕМЯ ТРЕНИРОВКИ", style = Caption)
                SwitchRow("Голосовые подсказки", "Название упражнения, отдых, смена стороны", s.voice) { v -> vm.updateSettings { it.copy(voice = v) } }
                SwitchRow("Звуковые сигналы", "Отсчёт 3-2-1 и конец подхода", s.sound) { v -> vm.updateSettings { it.copy(sound = v) } }
                SwitchRow("Вибрация", null, s.vibration) { v -> vm.updateSettings { it.copy(vibration = v) } }
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить все данные", color = Danger)
            }
            Text("Калистеника · версия 2.0 · все данные хранятся только на телефоне", style = Caption, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить всё?") },
            text = { Text("Удалятся тренировки, вес, привычки и настройки. Отменить нельзя.") },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false; onBack() }) { Text("Сбросить", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
}

@Composable
fun ProfileEditor(p: Profile, onChange: (Profile) -> Unit) {
    Card(color = Surface1) {
        Text("ПРОФИЛЬ", style = Caption)
        Stepper("Возраст", "${p.age} лет", { onChange(p.copy(age = (p.age - 1).coerceAtLeast(14))) }, { onChange(p.copy(age = (p.age + 1).coerceAtMost(90))) })
        Stepper("Рост", "${p.heightCm} см", { onChange(p.copy(heightCm = p.heightCm - 1)) }, { onChange(p.copy(heightCm = p.heightCm + 1)) })
        Stepper("Стартовый вес", "${kg(p.startWeight)} кг", { onChange(p.copy(startWeight = p.startWeight - 0.5)) }, { onChange(p.copy(startWeight = p.startWeight + 0.5)) })
        Stepper("Цель", "${kg(p.goalWeight)} кг", { onChange(p.copy(goalWeight = p.goalWeight - 0.5)) }, { onChange(p.copy(goalWeight = p.goalWeight + 0.5)) })
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onMinus, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−", fontSize = 20.sp) }
        Text(value, modifier = Modifier.padding(horizontal = 6.dp).size(width = 86.dp, height = 24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        FilledTonalButton(onClick = onPlus, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
fun DaysEditor(days: List<Int>, onChange: (List<Int>) -> Unit) {
    var picked by remember(days) { mutableStateOf(days.toSet()) }
    Card(color = Surface1) {
        Text("ДНИ ТРЕНИРОВОК", style = Caption)
        Text("Выберите 4 дня. Порядок: сила → пляж → круговая → кор и прогулка.", color = Muted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekdayShort.forEachIndexed { i, label ->
                val d = i + 1
                val on = d in picked
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (on) Accent else Surface2)
                        .clickable {
                            picked = if (on) picked - d else picked + d
                            if (picked.size == 4) onChange(picked.sorted())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (on) Color.White else Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        if (picked.size != 4) Text("Выбрано ${picked.size} из 4", color = Danger, fontSize = 13.sp)
    }
}

@Composable
fun StartEditor(startEpochDay: Long, onChange: (Long) -> Unit) {
    val start = LocalDate.ofEpochDay(startEpochDay)
    Card(color = Surface1) {
        Text("СТАРТ ПРОГРАММЫ", style = Caption)
        Text(start.longTitle(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Финиш: ${start.plusWeeks(12).minusDays(1).shortTitle()}", color = Muted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onChange(startEpochDay - 7) }) { Text("−1 нед.") }
            FilledTonalButton(onClick = { onChange(AppViewModel.thisMonday().toEpochDay()) }) { Text("Эта неделя") }
            FilledTonalButton(onClick = { onChange(startEpochDay + 7) }) { Text("+1 нед.") }
        }
    }
}

@Composable
fun EquipmentEditor(s: Settings, onChange: (Settings) -> Unit) {
    Card(color = Surface1) {
        Text("ИНВЕНТАРЬ", style = Caption)
        SwitchRow("Есть турник", "Подтягивания и висы. Без турника заменяются упражнениями на коврике", s.hasBar) { onChange(s.copy(hasBar = it)) }
        SwitchRow("Есть скамья, лежак или бордюр", "Для отжиманий от опоры и обратных отжиманий", s.hasBench) { onChange(s.copy(hasBench = it)) }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle != null) Text(subtitle, style = Caption)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface2).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (i == selected) Accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (i == selected) Color.White else Muted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun OnboardingScreen(initial: AppState, onDone: (Profile, Settings) -> Unit) {
    var profile by remember { mutableStateOf(initial.profile) }
    var settings by remember {
        mutableStateOf(initial.settings.copy(startEpochDay = AppViewModel.nextMonday().toEpochDay()))
    }
    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(brush = HeroGradient, padding = 20.dp) {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                MoveFigure("burpee", Modifier.fillMaxSize(), color = Color.White)
            }
            Text("Минус 15 кг за 12 недель", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "Программа на собственном весе: коврик, пляж и турник, если найдётся. 4 тренировки в неделю, шаги, питание и контроль веса.",
                color = Color.White.copy(alpha = 0.92f),
            )
        }
        ProfileEditor(profile) { profile = it }
        DaysEditor(settings.trainingDays) { settings = settings.copy(trainingDays = it) }
        StartEditor(settings.startEpochDay) { settings = settings.copy(startEpochDay = it) }
        EquipmentEditor(settings) { settings = it }
        Button(
            onClick = { onDone(profile, settings) },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Поехали", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
    }
}
