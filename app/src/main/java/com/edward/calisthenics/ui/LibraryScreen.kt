package com.edward.calisthenics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.Category
import com.edward.calisthenics.data.Equip
import com.edward.calisthenics.data.Exercises
import com.edward.calisthenics.data.Measure

@Composable
fun LibraryScreen(onExercise: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = Exercises.all.filter { filter == null || it.category.name == filter }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text("Упражнения", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("${Exercises.all.size} упражнений с анимацией техники", color = Muted)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Все") })
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = filter == c.name,
                            onClick = { filter = if (filter == c.name) null else c.name },
                            label = { Text(c.title) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = categoryColor(c).copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
        items(list, key = { it.id }) { ex ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .clickable { onExercise(ex.id) }
                    .padding(10.dp),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(16.dp)).background(Surface2)) {
                    MoveFigure(ex.anim, Modifier.fillMaxSize().padding(6.dp), color = categoryColor(ex.category))
                }
                Spacer(Modifier.height(8.dp))
                Text(ex.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                Text(ex.category.title + if (ex.equip != Equip.NONE) " · ${ex.equip.title.lowercase()}" else "", style = Caption, maxLines = 1)
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit, onTry: () -> Unit, onOpen: (String) -> Unit) {
    val ex = Exercises.get(id)
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(ex.name, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(28.dp)).background(CardGradient)) {
                MoveFigure(ex.anim, Modifier.fillMaxSize().padding(16.dp), color = categoryColor(ex.category))
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(ex.category.title, categoryColor(ex.category))
                    Pill(if (ex.measure == Measure.TIME) "на время" else "на повторы", Muted)
                }
            }
            Text(ex.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("Работают: ${ex.muscles}", color = Muted)
            if (ex.equip != Equip.NONE) Text("Нужно: ${ex.equip.title.lowercase()}", color = AccentSoft)

            Card(color = Surface1) {
                Text("ТЕХНИКА", style = Caption)
                ex.cues.forEach { Bullet(it, Success) }
            }
            if (ex.mistakes.isNotEmpty()) {
                Card(color = Surface1) {
                    Text("ЧАСТЫЕ ОШИБКИ", style = Caption)
                    ex.mistakes.forEach { Bullet(it, Danger) }
                }
            }
            val related = listOfNotNull(
                ex.easier?.let { "Полегче" to it },
                ex.harder?.let { "Посложнее" to it },
                ex.alt?.let { "Без инвентаря" to it },
            )
            if (related.isNotEmpty()) {
                Text("Варианты", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                related.forEach { (label, rid) ->
                    val r = Exercises.get(rid)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable { onOpen(rid) }.padding(10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Thumb(r.anim, size = 56.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(r.name, fontWeight = FontWeight.SemiBold)
                            Text(label, style = Caption)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onTry,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Text(if (ex.fixedVolume) "  Запустить таймер" else "  Сделать 3 подхода", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
