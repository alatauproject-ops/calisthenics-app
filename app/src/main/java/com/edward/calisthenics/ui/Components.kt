package com.edward.calisthenics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.calisthenics.data.Exercise
import com.edward.calisthenics.data.Item
import com.edward.calisthenics.data.Measure
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val Ru: Locale = Locale("ru")
val WeekdayShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

fun String.cap(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Ru) else it.toString() }
fun LocalDate.longTitle(): String = format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Ru)).cap()
fun LocalDate.shortTitle(): String = format(DateTimeFormatter.ofPattern("d MMMM", Ru))
fun LocalDate.monthTitle(): String = "${month.getDisplayName(TextStyle.FULL_STANDALONE, Ru).cap()} $year"
fun LocalDate.weekdayShort(): String = WeekdayShort[dayOfWeek.value - 1]

fun clock(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

fun minutesText(sec: Int): String {
    val m = (sec + 30) / 60
    return if (m >= 60) "${m / 60} ч ${m % 60} мин" else "$m мин"
}

fun valueText(ex: Exercise, value: Int): String = when {
    ex.measure == Measure.REPS -> "$value ${repsWord(value)}"
    value >= 120 && value % 60 == 0 -> "${value / 60} мин"
    value >= 60 -> clock(value)
    else -> "$value сек"
}

fun itemText(item: Item): String = valueText(item.exercise, item.value)

fun repsWord(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "повтор"
        m10 in 2..4 && m100 !in 12..14 -> "повтора"
        else -> "повторов"
    }
}

fun kg(v: Double): String = if (v % 1.0 == 0.0) "%.0f".format(Ru, v) else "%.1f".format(Ru, v)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    color: Color = Surface1,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    var m = modifier.fillMaxWidth().clip(shape)
    m = if (brush != null) m.background(brush) else m.background(color)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke(this)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = if (filled) Color.White else color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = TextMain) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Surface1).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, style = Caption, maxLines = 2)
    }
}

/** Кольцевой индикатор прогресса. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    track: Color = Surface2,
    content: @Composable () -> Unit = {},
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val d = size.minDimension - w
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Миниатюра упражнения в скруглённой плашке. */
@Composable
fun Thumb(animId: String, modifier: Modifier = Modifier, size: Dp = 64.dp, animate: Boolean = false) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(16.dp)).background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        MoveFigure(animId, Modifier.fillMaxSize().padding(4.dp), animate = animate)
    }
}

@Composable
fun Bullet(text: String, color: Color = Accent) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Text(text, modifier = Modifier.padding(start = 10.dp), color = TextMain)
    }
}
