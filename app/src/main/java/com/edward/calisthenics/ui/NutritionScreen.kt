package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.edward.calisthenics.data.Food
import com.edward.calisthenics.data.Recipe
import java.time.LocalDate
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
fun NutritionScreen(
    state: AppState,
    tab: Int,
    onTab: (Int) -> Unit,
    onToggleShop: (String) -> Unit,
    onClearShop: () -> Unit,
) {
    var recipe by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text("Питание", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("Меню", "Покупки", "Рецепты", "Цели"), tab, onTab)
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> MenuTab { recipe = it }
                1 -> ShoppingTab(state.shopping, onToggleShop, onClearShop)
                2 -> RecipesTab { recipe = it }
                else -> GoalsTab(state)
            }
        }
    }
    recipe?.let { RecipeDialog(Food.recipe(it)) { recipe = null } }
}

@Composable
private fun MenuTab(onRecipe: (String) -> Unit) {
    val todayDow = LocalDate.now().dayOfWeek.value
    var dow by rememberSaveable { mutableIntStateOf(todayDow) }
    val day = Food.week.getValue(dow)
    val (kcal, protein) = Food.dayTotals(day)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Меню повторяется каждую неделю, начиная с понедельника 2 ноября. Три ужина в кафе, остальное готовите дома.", color = Muted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekdayShort.forEachIndexed { i, label ->
                val d = i + 1
                val on = d == dow
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (on) Sea else Surface1)
                        .border(if (d == todayDow && !on) 2.dp else 0.dp, if (d == todayDow) Sea else Color.Transparent, CircleShape)
                        .clickable { dow = d },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (on) Color.White else Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("≈ $kcal", "ккал за день", Modifier.weight(1f), color = Accent)
            Stat("≈ $protein г", "белка", Modifier.weight(1f), color = Success)
        }
        day.ids().forEach { (meal, id) ->
            val r = Food.recipe(id)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).clickable { onRecipe(id) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(meal.title.uppercase(), style = Caption)
                        if (r.outside != null) Pill("в кафе", Rest) else Pill("дома", Success)
                    }
                    Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        "≈ ${r.kcal} ккал · ${r.protein} г белка" + (r.outside?.let { " · $it" } ?: if (r.minutes > 1) " · ${r.minutes} мин" else ""),
                        style = Caption,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
            }
        }
        Food.prepNotes[dow]?.let { note ->
            Card(color = Surface1) {
                Text("ЗАГОТОВКИ", style = Caption)
                Text(note)
                TextButton(onClick = { onRecipe("l_eggs_batch") }) { Text("Как варить рис и яйца впрок") }
            }
        }
        Text("Голодно? Добавьте овощи, огурцы, ещё яйцо или йогурт. Сладкие напитки и пиво не добавляйте.", style = Caption)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ShoppingTab(checked: List<String>, onToggle: (String) -> Unit, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(brush = SeaGradient) {
            Text("БЮДЖЕТ", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text(Food.budget, color = Color.White)
        }
        Food.shopping.forEach { g ->
            SectionTitle(g.title) {
                Text("${g.items.count { it.id in checked }}/${g.items.size}", style = Caption)
            }
            Text(g.note, color = Muted, fontSize = 14.sp)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1)) {
                g.items.forEach { item ->
                    val on = item.id in checked
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(item.id) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .background(if (on) Success else Color.Transparent)
                                .border(2.dp, if (on) Success else Surface3, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) Icon(Icons.Filled.Check, null, tint = Color(0xFF04150D), modifier = Modifier.size(16.dp))
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(item.name, color = if (on) Muted else TextMain, fontWeight = FontWeight.SemiBold)
                            Text("${item.local} · ${item.amount}", style = Caption)
                        }
                        Text(item.price, color = AccentSoft, fontSize = 13.sp)
                    }
                }
            }
        }
        if (checked.isNotEmpty()) {
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Снять все отметки") }
        }
        SectionTitle("Маленький холодильник")
        Card(color = Surface1) { Food.fridgeTips.forEach { Bullet(it, Rest) } }
        Text("Цены примерные, на рынке торгуются и бывает дешевле. Названия на вьетнамском можно показать продавцу.", style = Caption)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecipesTab(onRecipe: (String) -> Unit) {
    val groups = listOf(
        "Завтраки" to Food.recipes.filter { it.id.startsWith("b_") },
        "Обеды и ужины дома" to Food.recipes.filter { it.id.startsWith("l_") },
        "Перекусы" to Food.recipes.filter { it.id.startsWith("s_") },
        "В кафе" to Food.recipes.filter { it.id.startsWith("o_") },
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Всё готовится на одной сковороде или в кастрюле на газовой горелке, плюс чайник.", color = Muted, fontSize = 14.sp)
        groups.forEach { (title, list) ->
            SectionTitle(title)
            list.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable { onRecipe(r.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                r.outside != null -> "≈ ${r.kcal} ккал · ${r.outside}"
                                r.kcal == 0 -> "заготовка · ${r.minutes} мин"
                                else -> "≈ ${r.kcal} ккал · ${r.protein} г белка · ${r.minutes} мин"
                            },
                            style = Caption,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecipeDialog(r: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(r.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.kcal > 0) {
                    Text(
                        "≈ ${r.kcal} ккал · ${r.protein} г белка" + (r.outside?.let { " · $it" } ?: " · ${r.minutes} мин"),
                        color = AccentSoft, fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(if (r.outside != null) "ЧТО ЗАКАЗАТЬ" else "ПРОДУКТЫ", style = Caption, modifier = Modifier.padding(top = 4.dp))
                r.ingredients.forEach { Bullet(it, Success) }
                Text(if (r.outside != null) "СОВЕТ" else "КАК ГОТОВИТЬ", style = Caption, modifier = Modifier.padding(top = 4.dp))
                r.steps.forEachIndexed { i, st ->
                    Row {
                        Text("${i + 1}.", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                        Text(st)
                    }
                }
                r.tip?.let {
                    Text(it, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Понятно") } },
    )
}

@Composable
private fun GoalsTab(state: AppState) {
    val plan = Nutrition.plan(state.profile, state.currentWeight)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
