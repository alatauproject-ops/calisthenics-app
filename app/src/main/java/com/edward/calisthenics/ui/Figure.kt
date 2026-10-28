package com.edward.calisthenics.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import com.edward.calisthenics.data.Move
import com.edward.calisthenics.data.Moves
import com.edward.calisthenics.data.P
import com.edward.calisthenics.data.Skeleton

/**
 * Анимированная фигура, показывающая технику упражнения.
 * animate = false рисует статичный «рабочий» кадр (для списков).
 */
@Composable
fun MoveFigure(
    animId: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    color: Color = Accent,
    speed: Float = 1f,
) {
    val move = Moves[animId]
    if (move == null) {
        Canvas(modifier) {}
        return
    }
    if (animate) {
        val transition = rememberInfiniteTransition(label = animId)
        val cycleMs = (move.dur * 1000 / speed).toInt().coerceAtLeast(300)
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(cycleMs, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        Canvas(modifier) { drawMove(move, Moves.pose(move, phase.value), color) }
    } else {
        Canvas(modifier) { drawMove(move, Moves.still(move), color) }
    }
}

private fun DrawScope.drawMove(m: Move, s: Skeleton, color: Color) {
    val minX = m.box[0]
    val minY = m.box[1]
    val bw = (m.box[2] - m.box[0]).coerceAtLeast(0.2f)
    val bh = (m.box[3] - m.box[1]).coerceAtLeast(0.2f)
    // Не даём маленьким позам (лёжа) раздуваться: минимальный масштаб как у фигуры ростом ~1.1.
    val padX = size.width * 0.08f
    val padY = size.height * 0.08f
    val scale = minOf((size.width - 2 * padX) / bw, (size.height - 2 * padY) / maxOf(bh, 0.55f))
    val ox = (size.width - bw * scale) / 2 - minX * scale
    val oy = size.height - (size.height - bh * scale) / 2 + minY * scale
    fun pt(p: P) = Offset(ox + p.x * scale, oy - p.y * scale)
    fun pt(x: Float, y: Float) = Offset(ox + x * scale, oy - y * scale)

    val propColor = Color(0xFF3A4152)
    // Пол.
    drawLine(
        propColor.copy(alpha = 0.7f),
        pt(minX - 0.3f, 0f), pt(minX + bw + 0.3f, 0f),
        strokeWidth = 0.012f * scale, cap = StrokeCap.Round,
    )
    for (p in m.props) {
        when (p.type) {
            "mat" -> drawRoundRect(
                Color(0xFF2A3A55),
                topLeft = pt(p.x, 0.022f),
                size = Size(p.w * scale, 0.022f * scale),
                cornerRadius = CornerRadius(0.01f * scale),
            )
            "bench" -> {
                drawRoundRect(
                    Color(0xFF2A3040),
                    topLeft = pt(p.x, p.h),
                    size = Size(p.w * scale, p.h * scale),
                    cornerRadius = CornerRadius(0.02f * scale),
                )
                drawLine(Color(0xFF4A5368), pt(p.x, p.h), pt(p.x + p.w, p.h), strokeWidth = 0.02f * scale, cap = StrokeCap.Round)
            }
            "bar" -> {
                drawLine(propColor.copy(alpha = 0.5f), pt(p.x, p.y), pt(p.x, 0f), strokeWidth = 0.02f * scale)
                drawCircle(Color(0xFFB8C0D0), radius = 0.028f * scale, center = pt(p.x, p.y))
            }
        }
    }

    val limb = 0.056f * scale
    val far = if (m.front) color.copy(alpha = 0.85f) else lerpColor(color, Color(0xFF0D0F14), 0.45f)
    val bodyColor = color

    fun chain(vararg pts: P, c: Color, w: Float = limb) {
        val path = Path()
        path.moveTo(pt(pts[0]).x, pt(pts[0]).y)
        for (i in 1 until pts.size) path.lineTo(pt(pts[i]).x, pt(pts[i]).y)
        drawPath(path, c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // Дальние конечности.
    chain(s.hips[1], s.knees[1], s.ankles[1], s.toes[1], c = far)
    chain(s.shoulders[1], s.elbows[1], s.hands[1], c = far)
    // Корпус и голова.
    if (m.front) {
        chain(s.shoulders[0], s.shoulders[1], c = bodyColor, w = limb * 1.1f)
        chain(s.hips[0], s.hips[1], c = bodyColor, w = limb * 1.1f)
    }
    chain(s.hip, s.shoulder, c = bodyColor, w = limb * 1.65f)
    chain(s.shoulder, s.neck, c = bodyColor, w = limb * 0.9f)
    drawCircle(bodyColor, radius = Moves.HEAD * scale * 1.05f, center = pt(s.head))
    // Ближние конечности.
    chain(s.hips[0], s.knees[0], s.ankles[0], s.toes[0], c = color)
    chain(s.shoulders[0], s.elbows[0], s.hands[0], c = color)
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
