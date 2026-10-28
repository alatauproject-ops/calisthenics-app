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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    phase: Float? = null,
) {
    val move = Moves[animId]
    if (move == null) {
        Canvas(modifier) {}
        return
    }
    if (phase != null) {
        Canvas(modifier) { drawMove(move, Moves.pose(move, phase), color) }
    } else if (animate) {
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
    // Запас под толщину тела вокруг скелета.
    val minX = m.box[0] - 0.09f
    val minY = m.box[1] - 0.02f
    val bw = (m.box[2] - m.box[0] + 0.18f).coerceAtLeast(0.2f)
    val bh = (m.box[3] - m.box[1] + 0.11f).coerceAtLeast(0.2f)
    // Не даём маленьким позам (лёжа) раздуваться: минимальный масштаб как у фигуры ростом ~1.1.
    val padX = size.width * 0.04f
    val padY = size.height * 0.04f
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

    // Тень на полу.
    val feetX = (s.ankles[0].x + s.ankles[1].x + s.hip.x) / 3f
    drawOval(
        Color.Black.copy(alpha = 0.28f),
        topLeft = pt(feetX - 0.32f, 0.018f),
        size = Size(0.64f * scale, 0.036f * scale),
    )

    val skin = Color(0xFFE9B48C)
    val skinFar = darker(skin, 0.22f)
    val shirt = color
    val shirtFar = darker(color, 0.25f)
    val shorts = Color(0xFF2B3A5C)
    val shortsFar = darker(shorts, 0.25f)
    val shoe = Color(0xFFE8ECF3)
    val shoeFar = darker(shoe, 0.25f)
    val hair = Color(0xFF2B231E)

    /** Сужающийся «цилиндр» между двумя точками со светом сверху. */
    fun segment(a: P, b: P, ra: Float, rb: Float, c: Color) {
        val sa = pt(a)
        val sb = pt(b)
        val dx = sb.x - sa.x
        val dy = sb.y - sa.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        var nx = -dy / len
        var ny = dx / len
        if (ny > 0) { nx = -nx; ny = -ny } // нормаль смотрит вверх, к свету
        val pa = ra * scale
        val pb = rb * scale
        val brush = Brush.linearGradient(
            0f to lighter(c, 0.18f), 0.55f to c, 1f to darker(c, 0.22f),
            start = Offset((sa.x + sb.x) / 2 + nx * maxOf(pa, pb), (sa.y + sb.y) / 2 + ny * maxOf(pa, pb)),
            end = Offset((sa.x + sb.x) / 2 - nx * maxOf(pa, pb), (sa.y + sb.y) / 2 - ny * maxOf(pa, pb)),
        )
        val path = Path().apply {
            moveTo(sa.x + nx * pa, sa.y + ny * pa)
            lineTo(sb.x + nx * pb, sb.y + ny * pb)
            lineTo(sb.x - nx * pb, sb.y - ny * pb)
            lineTo(sa.x - nx * pa, sa.y - ny * pa)
            close()
        }
        drawPath(path, brush)
        drawCircle(brush, pa, sa)
        drawCircle(brush, pb, sb)
    }

    fun along(a: P, b: P, t: Float) = P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    fun leg(i: Int, near: Boolean) {
        val hip = s.hips[i]
        val knee = s.knees[i]
        val ankle = s.ankles[i]
        val toe = s.toes[i]
        segment(hip, knee, 0.056f, 0.040f, if (near) skin else skinFar)
        segment(knee, ankle, 0.040f, 0.026f, if (near) skin else skinFar)
        segment(ankle, toe, 0.030f, 0.022f, if (near) shoe else shoeFar)
        segment(hip, along(hip, knee, 0.55f), 0.064f, 0.050f, if (near) shorts else shortsFar)
    }

    fun arm(i: Int, near: Boolean) {
        val sh = s.shoulders[i]
        val el = s.elbows[i]
        val hand = s.hands[i]
        segment(sh, el, 0.034f, 0.027f, if (near) skin else skinFar)
        segment(el, hand, 0.027f, 0.021f, if (near) skin else skinFar)
        drawCircle(if (near) skin else skinFar, 0.027f * scale, pt(hand))
        segment(sh, along(sh, el, 0.38f), 0.043f, 0.036f, if (near) shirt else shirtFar)
    }

    fun torso() {
        val hip = s.hip
        val sh = s.shoulder
        if (m.front) {
            segment(s.hips[0], s.hips[1], 0.07f, 0.07f, shorts)
            segment(along(hip, sh, 0.25f), along(hip, sh, 0.8f), 0.115f, 0.13f, shirt)
            segment(s.shoulders[0], s.shoulders[1], 0.06f, 0.06f, shirt)
        } else {
            segment(hip, along(hip, sh, 0.3f), 0.075f, 0.078f, shorts)
            segment(along(hip, sh, 0.24f), along(hip, sh, 0.86f), 0.078f, 0.088f, shirt)
        }
        segment(sh, s.neck, 0.024f, 0.022f, skin)
    }

    fun head() {
        val c = pt(s.head)
        val r = Moves.HEAD * scale * 1.1f
        val dx = s.head.x - s.neck.x
        val dy = s.head.y - s.neck.y
        val up = kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
        drawCircle(
            Brush.radialGradient(listOf(lighter(skin, 0.1f), darker(skin, 0.15f)), center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.6f),
            r, c,
        )
        // Волосы: сверху и на затылке (в профиле лицо смотрит «по часовой» от макушки).
        val hairDir = if (m.front) up else up + 45f
        val sweep = if (m.front) 170f else 210f
        drawArc(
            hair, startAngle = -hairDir - sweep / 2, sweepAngle = sweep, useCenter = true,
            topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
        )
        val faceAng = if (m.front) up else up - 90f
        fun dirAt(deg: Float, k: Float) = Offset(
            c.x + kotlin.math.cos(Math.toRadians(deg.toDouble())).toFloat() * r * k,
            c.y - kotlin.math.sin(Math.toRadians(deg.toDouble())).toFloat() * r * k,
        )
        val eyeColor = Color(0xFF2B231E)
        if (m.front) {
            drawCircle(eyeColor, r * 0.11f, dirAt(up + 90f, 0.38f) + (dirAt(up, 0.1f) - c))
            drawCircle(eyeColor, r * 0.11f, dirAt(up - 90f, 0.38f) + (dirAt(up, 0.1f) - c))
        } else {
            drawCircle(eyeColor, r * 0.11f, dirAt(faceAng + 25f, 0.62f))
        }
    }

    // Порядок: дальние конечности, корпус и голова, ближние конечности.
    leg(1, near = m.front)
    arm(1, near = m.front)
    torso()
    head()
    leg(0, near = true)
    arm(0, near = true)
}

private fun darker(c: Color, t: Float) = lerpColor(c, Color.Black, t)
private fun lighter(c: Color, t: Float) = lerpColor(c, Color.White, t)

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
