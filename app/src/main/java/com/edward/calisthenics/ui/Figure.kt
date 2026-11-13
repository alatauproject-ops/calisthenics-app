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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.edward.calisthenics.data.Move
import com.edward.calisthenics.data.Moves
import com.edward.calisthenics.data.P
import com.edward.calisthenics.data.Skeleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Какие мышцы подсвечивать в анимации. Ключ — id анимации. */
private val MUSCLES: Map<String, List<String>> = mapOf(
    "pushup" to listOf("chest", "triceps", "abs"), "knee_pushup" to listOf("chest", "triceps"),
    "incline_pushup" to listOf("chest", "triceps"), "decline_pushup" to listOf("chest", "shoulders"),
    "pike_pushup" to listOf("shoulders", "triceps"), "bench_dip" to listOf("triceps"),
    "squat" to listOf("quads", "glutes"), "squat_hold" to listOf("quads", "glutes"), "squat_jump" to listOf("quads", "glutes", "calves"),
    "reverse_lunge" to listOf("quads", "glutes"), "jump_lunge" to listOf("quads", "glutes"), "calf_raise" to listOf("calves"),
    "glute_bridge" to listOf("glutes", "hams"), "bridge_hold" to listOf("glutes", "hams"),
    "plank" to listOf("abs", "shoulders"), "high_plank" to listOf("abs", "shoulders"), "side_plank" to listOf("obliques", "shoulders"),
    "hollow_hold" to listOf("abs"), "crunch" to listOf("abs"), "leg_raises" to listOf("abs"), "dead_bug" to listOf("abs"),
    "mountain_climber" to listOf("abs", "shoulders"), "superman" to listOf("back", "glutes"), "superman_pull" to listOf("back"),
    "australian_row" to listOf("back", "biceps"), "pullup" to listOf("back", "biceps"), "negative_pullup" to listOf("back", "biceps"),
    "dead_hang" to listOf("back"), "hanging_knee_raise" to listOf("abs"),
    "high_knees" to listOf("quads", "calves"), "burpee" to listOf("quads", "chest"), "burpee_low" to listOf("quads", "chest"),
    "down_dog" to listOf("hams", "calves", "shoulders"), "knee_elbow" to listOf("abs", "obliques"),
    "seated_knee_tuck" to listOf("abs"), "bear_crawl" to listOf("shoulders", "quads", "abs"),
    "beast" to listOf("shoulders", "abs", "quads"), "dive" to listOf("chest", "shoulders", "triceps"),
    "kickthrough" to listOf("abs", "obliques", "shoulders"), "hand_foot_tap" to listOf("abs", "obliques"),
    "psoas_stretch" to listOf("quads"), "ninety_ninety" to listOf("glutes"), "taichi_rooster" to listOf("glutes", "quads"),
)

/**
 * Анимированная фигура, показывающая технику упражнения.
 * animate = false рисует статичный «рабочий» кадр (для списков).
 * guide = true добавляет полупрозрачный силуэт начала движения и стрелку.
 */
@Composable
fun MoveFigure(
    animId: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    color: Color = Accent,
    speed: Float = 1f,
    phase: Float? = null,
    guide: Boolean = false,
    muscles: Boolean = true,
    floor: Boolean = true,
) {
    val move = Moves[animId]
    if (move == null) {
        Canvas(modifier) {}
        return
    }
    val hl = if (muscles) MUSCLES[animId].orEmpty() else emptyList()
    if (phase != null) {
        Canvas(modifier) { drawMove(move, Moves.pose(move, phase), color, hl, guide, floor) }
    } else if (animate) {
        val transition = rememberInfiniteTransition(label = animId)
        val cycleMs = (move.dur * 1000 / speed).toInt().coerceAtLeast(300)
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(cycleMs, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        Canvas(modifier) { drawMove(move, Moves.pose(move, phase.value), color, hl, guide, floor) }
    } else {
        Canvas(modifier) { drawMove(move, Moves.still(move), color, hl, guide, floor) }
    }
}

private operator fun P.plus(o: P) = P(x + o.x, y + o.y)
private operator fun P.minus(o: P) = P(x - o.x, y - o.y)
private operator fun P.times(k: Float) = P(x * k, y * k)
private fun P.len() = hypot(x, y).coerceAtLeast(1e-6f)
private fun P.unit() = this * (1f / len())
/** Нормаль: s > 0 — против часовой. */
private fun P.normal(s: Int) = if (s > 0) P(-y, x) else P(y, -x)
private fun lerpP(a: P, b: P, t: Float) = P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun DrawScope.drawMove(m: Move, s: Skeleton, color: Color, muscles: List<String>, guide: Boolean, floor: Boolean) {
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
    if (floor) {
        drawLine(
            propColor.copy(alpha = 0.7f),
            pt(minX - 0.3f, 0f), pt(minX + bw + 0.3f, 0f),
            strokeWidth = 0.012f * scale, cap = StrokeCap.Round,
        )
    }
    for (p in m.props) {
        when (p.type) {
            "mat" -> drawRoundRect(
                Color(0xFF2F4A73),
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
        Color.Black.copy(alpha = 0.3f),
        topLeft = pt(feetX - 0.34f, 0.02f),
        size = Size(0.68f * scale, 0.04f * scale),
    )

    val bounds = Rect(0f, 0f, size.width, size.height)
    if (guide && m.frames.size > 1) {
        val s0 = Moves.pose(m, 0f)
        drawIntoCanvas { it.saveLayer(bounds, Paint().apply { alpha = 0.28f }) }
        drawBody(m, s0, color, scale, ::pt, ghost = true)
        drawIntoCanvas { it.restore() }
        drawGuideArrow(s0, Moves.still(m), scale, ::pt)
    }

    drawIntoCanvas { it.saveLayer(bounds, Paint()) }
    drawBody(m, s, color, scale, ::pt, ghost = false)
    if (muscles.isNotEmpty()) drawMuscles(m, s, muscles, scale, ::pt)
    drawIntoCanvas { it.restore() }
}

/** Стрелка от самой подвижной точки начальной позы к рабочей. */
private fun DrawScope.drawGuideArrow(a: Skeleton, b: Skeleton, scale: Float, pt: (P) -> Offset) {
    val cand = listOf(a.head to b.head, a.hip to b.hip, a.hands[0] to b.hands[0], a.knees[0] to b.knees[0], a.ankles[0] to b.ankles[0])
    val (from, to) = cand.maxBy { (it.second - it.first).len() }
    if ((to - from).len() < 0.05f) return
    val pa = pt(from)
    val pb = pt(to)
    val dx = pb.x - pa.x
    val dy = pb.y - pa.y
    val l = hypot(dx, dy)
    val nx = -dy / l
    val ny = dx / l
    val off = 0.09f * scale
    val a1 = Offset(pa.x + nx * off, pa.y + ny * off)
    val b1 = Offset(pb.x + nx * off, pb.y + ny * off)
    val c = Offset((a1.x + b1.x) / 2 + nx * l * 0.18f, (a1.y + b1.y) / 2 + ny * l * 0.18f)
    val arrow = Color(0xFFFFC857)
    val path = Path().apply {
        moveTo(a1.x, a1.y)
        quadraticBezierTo(c.x, c.y, b1.x, b1.y)
    }
    drawPath(
        path, arrow,
        style = Stroke(
            width = 0.016f * scale, cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.03f * scale, 0.025f * scale)),
        ),
    )
    val ang = atan2(b1.y - c.y, b1.x - c.x)
    val hs = 0.05f * scale
    val head = Path().apply {
        moveTo(b1.x + cos(ang) * hs * 0.6f, b1.y + sin(ang) * hs * 0.6f)
        lineTo(b1.x + cos(ang + 2.5f) * hs, b1.y + sin(ang + 2.5f) * hs)
        lineTo(b1.x + cos(ang - 2.5f) * hs, b1.y + sin(ang - 2.5f) * hs)
        close()
    }
    drawPath(head, arrow)
}

/** Мягкая красная подсветка работающих мышц поверх тела (только там, где тело). */
private fun DrawScope.drawMuscles(m: Move, s: Skeleton, muscles: List<String>, scale: Float, pt: (P) -> Offset) {
    val h = s.hip
    val ax = (s.shoulder - h).unit()
    val l = (s.shoulder - h).len()
    val f = if (m.front) P(0f, 0f) else ax.normal(-1)
    fun t(tt: Float, w: Float) = h + ax * (l * tt) + f * w
    val spots = buildList {
        for (k in muscles) when (k) {
            "chest" -> add(t(0.75f, 0.06f))
            "abs" -> add(t(0.45f, 0.06f))
            "obliques" -> add(t(0.45f, 0f))
            "back" -> add(t(0.7f, -0.05f))
            "glutes" -> add(t(0f, -0.06f))
            "shoulders" -> add(s.shoulders[0])
            "quads" -> { add(lerpP(s.hips[0], s.knees[0], 0.5f)); add(lerpP(s.hips[1], s.knees[1], 0.5f)) }
            "hams" -> add(lerpP(s.hips[0], s.knees[0], 0.45f))
            "calves" -> { add(lerpP(s.knees[0], s.ankles[0], 0.35f)); add(lerpP(s.knees[1], s.ankles[1], 0.35f)) }
            "triceps", "biceps" -> add(lerpP(s.shoulders[0], s.elbows[0], 0.5f))
        }
    }
    val r = 0.075f * scale
    for (p in spots) {
        val c = pt(p)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xBFFF283C), Color(0x00FF283C)), center = c, radius = r),
            radius = r, center = c, blendMode = BlendMode.SrcAtop,
        )
    }
}

/** Тело с контуром: голени с икрами, бёдра, шорты, корпус с грудью, кисти, кроссовки, голова с лицом. */
private fun DrawScope.drawBody(m: Move, s: Skeleton, color: Color, scale: Float, pt: (P) -> Offset, ghost: Boolean) {
    val outline = Color(0xFF14161C)
    val lw = maxOf(1.2f, 0.0075f * scale)
    val ghostColor = Color(0xFF9FB3D9)
    val skin = Color(0xFFE8AE84)
    val skinFar = darker(skin, 0.2f)
    val shirt = color
    val shirtFar = darker(color, 0.25f)
    val shorts = Color(0xFF2B3A5C)
    val shortsFar = darker(shorts, 0.28f)
    val shoe = Color(0xFFF1F3F8)
    val shoeFar = darker(shoe, 0.22f)
    val hair = Color(0xFF2B231E)

    /** Замкнутая фигура по точкам; curve сглаживает углы квадратичными кривыми. */
    fun paint(pts: List<P>, c: Color, curve: Boolean = true) {
        val q = pts.map(pt)
        val path = Path()
        if (curve) {
            val first = q[0]
            val last = q[q.lastIndex]
            path.moveTo((first.x + last.x) / 2, (first.y + last.y) / 2)
            for (i in q.indices) {
                val a = q[i]
                val b = q[(i + 1) % q.size]
                path.quadraticBezierTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2)
            }
        } else {
            path.moveTo(q[0].x, q[0].y)
            for (i in 1 until q.size) path.lineTo(q[i].x, q[i].y)
        }
        path.close()
        if (ghost) {
            drawPath(path, ghostColor)
            return
        }
        val minX = q.minOf { it.x }
        val minY = q.minOf { it.y }
        val maxX = q.maxOf { it.x }
        val maxY = q.maxOf { it.y }
        val brush = Brush.linearGradient(
            0f to lighter(c, 0.16f), 0.6f to c, 1f to darker(c, 0.2f),
            start = Offset(minX, minY), end = Offset(maxX, maxY),
        )
        drawPath(path, brush)
        drawPath(path, outline, style = Stroke(width = lw, join = StrokeJoin.Round))
    }

    /** Конечность с «брюшком» мышцы в точке bt; cap — насколько скруглён конец. */
    fun limb(a: P, b: P, ra: Float, rm: Float, rb: Float, c: Color, bt: Float = 0.35f, cap: Float = 0.9f) {
        val d = (b - a).unit()
        val n = d.normal(1)
        val l = (b - a).len()
        val mid = a + d * (l * bt)
        paint(
            listOf(
                a + n * ra, mid + n * rm, b + n * rb, b + d * (rb * cap),
                b - n * rb, mid - n * rm, a - n * ra, a - d * (ra * 0.9f),
            ),
            c,
        )
    }

    fun hand(el: P, h: P, c: Color) {
        val d = (h - el).unit()
        val n = d.normal(1)
        val p = h + d * 0.012f
        paint(
            listOf(p + n * 0.024f, p + d * 0.035f + n * 0.02f, p + d * 0.05f, p + d * 0.035f - n * 0.022f, p - n * 0.024f, p - d * 0.012f),
            c,
        )
    }

    fun foot(knee: P, ankle: P, toe: P, c: Color) {
        val t = (toe - ankle).unit()
        var u = t.normal(1)
        val shin = (knee - ankle).unit()
        if (u.x * shin.x + u.y * shin.y < 0) u = u * -1f
        val heel = ankle - t * 0.03f
        val tip = toe + t * 0.012f
        paint(
            listOf(ankle + u * 0.035f, ankle + t * 0.04f + u * 0.02f, tip + u * 0.006f, tip - u * 0.02f, heel - u * 0.022f, heel + u * 0.012f),
            c,
        )
        if (!ghost) drawLine(Color(0xFFC8CCD6), pt(heel - u * 0.022f), pt(tip - u * 0.02f), strokeWidth = lw * 1.4f, cap = StrokeCap.Round)
    }

    fun leg(i: Int, near: Boolean) {
        val hip = s.hips[i]
        val knee = s.knees[i]
        val ankle = s.ankles[i]
        limb(knee, ankle, 0.039f, 0.043f, 0.025f, if (near) skin else skinFar, 0.3f)
        foot(knee, ankle, s.toes[i], if (near) shoe else shoeFar)
        limb(hip, knee, 0.06f, 0.062f, 0.04f, if (near) skin else skinFar, 0.3f)
        val half = hip + (knee - hip).unit() * ((knee - hip).len() * 0.5f)
        limb(hip, half, 0.064f, 0.062f, 0.056f, if (near) shorts else shortsFar, 0.5f, 0.15f)
    }

    fun arm(i: Int, near: Boolean) {
        val sh = s.shoulders[i]
        val el = s.elbows[i]
        val h = s.hands[i]
        limb(el, h, 0.028f, 0.03f, 0.021f, if (near) skin else skinFar, 0.3f)
        hand(el, h, if (near) skin else skinFar)
        limb(sh, el, 0.038f, 0.036f, 0.026f, if (near) skin else skinFar, 0.35f)
        val sleeve = sh + (el - sh).unit() * ((el - sh).len() * 0.4f)
        limb(sh, sleeve, 0.045f, 0.044f, 0.04f, if (near) shirt else shirtFar, 0.5f, 0.15f)
    }

    fun torso() {
        val h = s.hip
        val ax = (s.shoulder - h).unit()
        val l = (s.shoulder - h).len()
        val f = ax.normal(-1)
        fun at(t: Float, w: Float) = h + ax * (l * t) + f * w
        if (m.front) {
            paint(listOf(at(0f, -0.085f), at(0.45f, -0.095f), at(0.8f, -0.12f), at(1.04f, -0.10f), at(1.04f, 0.10f), at(0.8f, 0.12f), at(0.45f, 0.095f), at(0f, 0.085f)), shirt)
            paint(listOf(at(-0.12f, -0.09f), at(0.3f, -0.092f), at(0.3f, 0.092f), at(-0.12f, 0.09f)), shorts, curve = false)
        } else {
            paint(listOf(at(-0.05f, -0.075f), at(0.45f, -0.072f), at(0.8f, -0.085f), at(1.02f, -0.055f), at(1.06f, 0.02f), at(0.82f, 0.098f), at(0.5f, 0.08f), at(0.15f, 0.072f), at(-0.05f, 0.07f)), shirt)
            paint(listOf(at(-0.08f, -0.078f), at(0.3f, -0.076f), at(0.3f, 0.077f), at(-0.08f, 0.072f)), shorts, curve = false)
        }
        limb(s.shoulder, s.neck, 0.027f, 0.026f, 0.024f, skin, 0.5f)
    }

    fun head() {
        val c = pt(s.head)
        val r = Moves.HEAD * scale * 1.12f
        val up = atan2(s.head.y - s.neck.y, s.head.x - s.neck.x) * 180f / Math.PI.toFloat()
        fun at(deg: Float, k: Float) = Offset(
            c.x + cos(Math.toRadians(deg.toDouble())).toFloat() * r * k,
            c.y - sin(Math.toRadians(deg.toDouble())).toFloat() * r * k,
        )
        val face = if (m.front) up else up - 90f
        if (!m.front) {
            val nose = Path().apply {
                val a = at(face + 5f, 1.0f)
                val b = at(face + 2f, 1.22f)
                val d = at(face - 12f, 0.98f)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); close()
            }
            drawPath(nose, if (ghost) ghostColor else skin)
            if (!ghost) drawPath(nose, outline, style = Stroke(width = lw, join = StrokeJoin.Round))
        }
        if (ghost) {
            drawCircle(ghostColor, r, c)
            return
        }
        drawCircle(
            Brush.radialGradient(listOf(lighter(skin, 0.12f), darker(skin, 0.18f)), center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.5f),
            r, c,
        )
        drawCircle(outline, r, c, style = Stroke(width = lw))
        // Волосы.
        val hairDir = if (m.front) up else up + 40f
        val sweep = if (m.front) 150f else 200f
        drawArc(
            hair, startAngle = -hairDir - sweep / 2, sweepAngle = sweep, useCenter = true,
            topLeft = Offset(c.x - r * 1.04f, c.y - r * 1.04f), size = Size(2 * r * 1.04f, 2 * r * 1.04f),
        )
        if (m.front) {
            val shift = at(up, 0.08f) - c
            drawCircle(hair, r * 0.1f, at(up + 90f, 0.4f) + shift)
            drawCircle(hair, r * 0.1f, at(up - 90f, 0.4f) + shift)
            for (d in listOf(up + 90f, up - 90f)) drawCircle(darker(skin, 0.1f), r * 0.18f, at(d, 1.0f))
            val mouth = at(up + 180f, 0.45f)
            drawArc(
                Color(0xFF8A4B3A), startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(mouth.x - r * 0.18f, mouth.y - r * 0.23f), size = Size(r * 0.36f, r * 0.36f),
                style = Stroke(width = lw),
            )
        } else {
            drawCircle(hair, r * 0.1f, at(face + 28f, 0.62f))
            drawLine(hair, at(face + 40f, 0.62f), at(face + 18f, 0.78f), strokeWidth = lw * 1.2f, cap = StrokeCap.Round)
            val ear = at(face + 170f, 0.12f)
            drawOval(darker(skin, 0.08f), topLeft = Offset(ear.x - r * 0.17f, ear.y - r * 0.24f), size = Size(r * 0.34f, r * 0.48f))
            drawOval(outline, topLeft = Offset(ear.x - r * 0.17f, ear.y - r * 0.24f), size = Size(r * 0.34f, r * 0.48f), style = Stroke(width = lw * 0.8f))
            drawLine(Color(0xFF8A4B3A), at(face - 40f, 0.8f), at(face - 28f, 0.86f), strokeWidth = lw, cap = StrokeCap.Round)
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
