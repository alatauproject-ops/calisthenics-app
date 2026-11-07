package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.Recipe
import com.edward.calisthenics.data.Steamer
import com.edward.calisthenics.data.TimeRow

/** Раздел про электрокастрюлю-пароварку Mishio MK321. */
@Composable
fun SteamerTab(onRecipe: (String) -> Unit) {
    var part by rememberSaveable { mutableIntStateOf(0) }
    val parts = listOf("Основы", "Яйца", "Утро", "На пару", "Рецепты", "Заготовки", "Уход")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(brush = SeaGradient) {
            Text("MISHIO MK321 · 1,5 Л · 600/300 ВТ", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("Пароварка: яйца, разогрев, курица, рыба, овощи", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Без масла и сахара, дёшево, одна кастрюля на всё.", color = Color.White.copy(alpha = 0.92f))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            parts.forEachIndexed { i, label ->
                val on = i == part
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (on) Sea else Color.Transparent)
                        .border(1.dp, if (on) Sea else Surface3, RoundedCornerShape(12.dp))
                        .clickable { part = i }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) { Text(label, color = if (on) Color.White else Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        when (part) {
            0 -> Basics()
            1 -> Eggs(onRecipe)
            2 -> Morning(onRecipe)
            3 -> SteamTimes()
            4 -> SteamRecipes(onRecipe)
            5 -> Prep(onRecipe)
            else -> Care()
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Basics() {
    SectionTitle("Как устроена")
    Card { Steamer.specs.forEach { Bullet(it, Sea) } }
    SectionTitle("Пять правил пара")
    Card { Steamer.golden.forEachIndexed { i, s -> Numbered(i + 1, s) } }
    SectionTitle("Посуда на пару")
    Card { Steamer.containers.forEach { Bullet(it, Rest) } }
    Text("Разделы выше: «Яйца» — время варки, «Утро» — завтрак за 15 минут, «На пару» — таблица времени по продуктам, «Заготовки» — контейнеры на неделю.", style = Caption)
}

@Composable
private fun Eggs(onRecipe: (String) -> Unit) {
    Text("Время от момента, когда яйца легли на вставку над кипящей водой, 600 Вт.", color = Muted, fontSize = 14.sp)
    Table(Steamer.eggTable, Accent)
    SectionTitle("Советы")
    Card { Steamer.eggTips.forEach { Bullet(it, Accent) } }
    SectionTitle("Рецепты с яйцами")
    RecipeRows(listOf("p_eggs", "p_egg_custard", "p_egg_breakfast", "p_oats_eggs"), onRecipe)
}

@Composable
private fun Morning(onRecipe: (String) -> Unit) {
    SectionTitle("Яйца и разогрев за один раз")
    Card { Steamer.morning.forEachIndexed { i, s -> Numbered(i + 1, s) } }
    RecipeRows(listOf("p_egg_breakfast", "p_oats_eggs"), onRecipe)
    SectionTitle("Сколько разогревать")
    Text("На вставке над кипящей водой, в миске или стеклянном контейнере без крышки.", color = Muted, fontSize = 14.sp)
    Table(Steamer.reheatTable, Rest)
}

@Composable
private fun SteamTimes() {
    Text("600 Вт, еда на вставке над кипящей водой, крышка закрыта. Кусочки одного размера готовятся одинаково.", color = Muted, fontSize = 14.sp)
    Table(Steamer.steamTable, Success)
    Text("Разное время? Кладите сначала долгое (батат, курица), короткое (рыба, брокколи) добавляйте сверху ближе к концу.", style = Caption)
}

@Composable
private fun SteamRecipes(onRecipe: (String) -> Unit) {
    val groups = listOf(
        "Яйца и завтраки" to listOf("p_eggs", "p_egg_custard", "p_egg_breakfast", "p_oats_eggs"),
        "Курица" to listOf("p_chicken_ginger", "p_chicken_sa", "p_chicken_prep", "p_chao", "p_canh"),
        "Рыба и море" to listOf("p_fish_ginger", "p_fish_tomato", "p_fish_sa", "p_shrimp", "p_squid"),
        "Тофу и овощи" to listOf("p_tofu", "p_veg", "p_eggplant"),
        "Гарниры" to listOf("p_rice", "p_sweet"),
    )
    groups.forEach { (title, ids) ->
        SectionTitle(title)
        RecipeRows(ids, onRecipe)
    }
}

@Composable
private fun Prep(onRecipe: (String) -> Unit) {
    Card(color = Surface1) {
        Text("БЮДЖЕТ", style = Caption)
        Text(Steamer.budget)
    }
    SectionTitle("Неделя заготовок")
    Card {
        var n = 0
        Steamer.weekPrep.forEach { s ->
            if (s == s.uppercase()) {
                n = 0
                Text(s, color = AccentSoft, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            } else {
                n++
                Numbered(n, s)
            }
        }
    }
    RecipeRows(listOf("p_chicken_prep", "p_eggs", "p_rice", "p_sweet", "p_veg"), onRecipe)
    SectionTitle("Контейнеры: собери обед")
    Steamer.boxes.forEach { b ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.title, fontWeight = FontWeight.SemiBold)
                    Text(b.vn, color = AccentSoft, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${b.kcal} ккал", color = Accent, fontWeight = FontWeight.Bold)
                    Text("${b.protein} г белка", color = Success, fontSize = 13.sp)
                }
            }
            Text(b.parts, style = Caption, modifier = Modifier.padding(top = 4.dp))
        }
    }
    SectionTitle("Сколько хранится")
    Table(Steamer.keepTable, Rest)
    SectionTitle("Купить на неделю")
    Table(Steamer.shopping, Success)
}

@Composable
private fun Care() {
    SectionTitle("Уход и безопасность")
    Card { Steamer.care.forEach { Bullet(it, Rest) } }
    SectionTitle("Частые ошибки")
    Card { Steamer.mistakes.forEach { Bullet(it, Danger) } }
}

@Composable
private fun Numbered(n: Int, text: String) {
    Row {
        Text("$n.", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
        Text(text)
    }
}

@Composable
private fun Table(rows: List<TimeRow>, color: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1)) {
        rows.forEachIndexed { i, r ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Surface2))
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.ru, fontWeight = FontWeight.SemiBold)
                    Text(r.vn, color = AccentSoft, fontSize = 13.sp)
                    if (r.note.isNotEmpty()) Text(r.note, style = Caption)
                }
                Text(r.time, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun RecipeRows(ids: List<String>, onRecipe: (String) -> Unit) {
    val byId = Steamer.recipes.associateBy { it.id }
    ids.mapNotNull { byId[it] }.forEach { r -> RecipeRow(r) { onRecipe(r.id) } }
}

@Composable
private fun RecipeRow(r: Recipe, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(r.name, fontWeight = FontWeight.SemiBold)
            Text(r.vn, color = AccentSoft, fontSize = 13.sp)
            Text(
                if (r.kcal == 0) "заготовка · ${r.minutes} мин" else "≈ ${r.kcal} ккал · ${r.protein} г белка · ${r.minutes} мин",
                style = Caption,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
    }
}
