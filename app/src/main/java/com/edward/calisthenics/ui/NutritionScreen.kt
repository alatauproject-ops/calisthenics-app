package com.edward.calisthenics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Nutrition
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(state: AppState) {
    val plan = Nutrition.plan(state.profile, state.currentWeight)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Питание", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Пересчитывается по последнему весу. Похудение на 80% делает питание, тренировки сохраняют мышцы и ускоряют процесс.", color = Muted)

        Card(brush = SeaGradient, padding = 20.dp) {
            Text("ЦЕЛЬ НА ДЕНЬ", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${plan.kcal}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(" ккал", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                "Расход ≈ ${plan.tdee} ккал, дефицит ≈ ${plan.tdee - plan.kcal}. Только за счёт питания это ≈ ${"%.1f".format(Ru, plan.weeklyLossKg)} кг в неделю, тренировки и шаги добавляют ещё.",
                color = Color.White.copy(alpha = 0.92f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Macro("Белок", plan.protein, Color(0xFFFF7A3D), Modifier.weight(1f))
            Macro("Жиры", plan.fat, Color(0xFFFFC857), Modifier.weight(1f))
            Macro("Углеводы", plan.carbs, Color(0xFF4FC3F7), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${kg(plan.waterL)} л", "воды в день (жара!)", Modifier.weight(1f), color = Rest)
            Stat("${(plan.protein / 4.0).roundToInt()} г", "белка на приём, 4 раза", Modifier.weight(1f))
        }

        Card(color = Surface1) {
            Text("РЕАЛИСТИЧНЫЙ ПРОГНОЗ", style = Caption)
            Text("−10…−14 кг за 12 недель", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Success)
            Text(
                "Первые 1–2 недели вес уходит быстро (вода), дальше 0,7–1 кг в неделю. Цель 85 кг реальна при соблюдении калорий; 82 кг, скорее всего, к 14–16 неделе. Не урезайте калории сильнее: потеряете мышцы и сорвётесь.",
                color = Muted,
            )
        }

        SectionTitle("Пример дня")
        Card(color = Surface1) {
            Nutrition.sampleDay.forEach { (meal, text) ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(meal, fontWeight = FontWeight.Bold, color = AccentSoft)
                    Text(text)
                }
            }
        }

        SectionTitle("Что брать во Вьетнаме")
        Card(color = Surface1) { Nutrition.choose.forEach { Bullet(it, Success) } }

        SectionTitle("Что ограничить")
        Card(color = Surface1) { Nutrition.limit.forEach { Bullet(it, Danger) } }

        SectionTitle("Советы")
        Card(color = Surface1) { Nutrition.tips.forEach { Bullet(it, Rest) } }

        Text(
            "Расчёт по формуле Миффлина — Сан-Жеора: возраст ${state.profile.age}, рост ${state.profile.heightCm} см, вес ${kg(state.currentWeight)} кг, активность ×1,5, дефицит 25%. Возраст и рост меняются в настройках.",
            style = Caption,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Macro(title: String, grams: Int, color: Color, modifier: Modifier) {
    Column(modifier) {
        Ring(progress = 1f, color = color, modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally), stroke = 8.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$grams", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("г", style = Caption)
            }
        }
        Text(title, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), fontWeight = FontWeight.SemiBold)
    }
}
