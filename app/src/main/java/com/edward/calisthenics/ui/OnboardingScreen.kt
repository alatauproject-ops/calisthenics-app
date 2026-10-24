package com.edward.calisthenics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.Level

private val levelHints = mapOf(
    Level.BEGINNER to "Пока не получается 10 отжиманий и ни одного подтягивания. Начнём с облегчённых вариантов.",
    Level.INTERMEDIATE to "Делаете 10–20 отжиманий и несколько подтягиваний. Начнём с классики.",
    Level.ADVANCED to "Свободно 20+ отжиманий и 10+ подтягиваний. Начнём со сложных вариантов.",
)

@Composable
fun OnboardingScreen(onDone: (Level, Set<Int>) -> Unit) {
    var level by remember { mutableStateOf(Level.BEGINNER) }
    var days by remember { mutableStateOf(setOf(1, 3, 5)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("💪", fontSize = 56.sp)
        Text("Калистеника", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Тренировки с собственным весом: программа с прогрессиями, график, счётчики повторений и таймеры.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard {
            Text("Ваш уровень", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LevelPicker(level) { level = it }
            Text(levelHints.getValue(level), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard {
            Text("Дни тренировок", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            DaysPicker(days) { days = it }
        }

        Button(
            onClick = { onDone(level, days) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Начать", fontSize = 18.sp) }
    }
}
