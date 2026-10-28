package com.edward.calisthenics.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Анимации техники из assets/moves.json (генерирует tools/moves.py).
 * Кадр задаёт таз, наклон корпуса, кисти и стопы; локти и колени считаются обратной кинематикой.
 */
@Serializable
data class MoveFrame(
    val hip: List<Float>,
    val torso: Float,
    val hands: List<List<Float>>,
    val feet: List<List<Float>>,
    val neck: Float? = null,
    val w: Float = 1f,
    val bend: List<Int>? = null,
    val foot: List<Float>? = null,
)

@Serializable
data class MoveProp(
    val type: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
)

@Serializable
data class Move(
    val dur: Float,
    val frames: List<MoveFrame>,
    val props: List<MoveProp> = emptyList(),
    val bend: List<Int> = listOf(-1, -1, 1, 1),
    val front: Boolean = false,
    val box: List<Float>,
)

@Serializable
private data class MoveFile(val moves: Map<String, Move>)

data class P(val x: Float, val y: Float)

/** Готовый скелет для отрисовки. Индекс 0 — ближние к зрителю конечности. */
class Skeleton(
    val hip: P,
    val shoulder: P,
    val neck: P,
    val head: P,
    val shoulders: List<P>,
    val hips: List<P>,
    val elbows: List<P>,
    val hands: List<P>,
    val knees: List<P>,
    val ankles: List<P>,
    val toes: List<P>,
)

object Moves {
    const val THIGH = 0.25f
    const val SHIN = 0.25f
    const val TORSO = 0.30f
    const val NECK = 0.05f
    const val HEAD = 0.065f
    const val UPPER = 0.17f
    const val FORE = 0.17f
    private const val SHOULDER_HALF = 0.10f
    private const val HIP_HALF = 0.065f

    private var moves: Map<String, Move> = emptyMap()

    fun load(context: Context) {
        if (moves.isNotEmpty()) return
        moves = runCatching {
            val text = context.assets.open("moves.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString(MoveFile.serializer(), text).moves
        }.getOrDefault(emptyMap())
    }

    operator fun get(id: String): Move? = moves[id]

    private fun rad(deg: Float) = deg * PI.toFloat() / 180f
    private fun dir(deg: Float) = P(cos(rad(deg)), sin(rad(deg)))
    private fun add(a: P, v: P, k: Float = 1f) = P(a.x + v.x * k, a.y + v.y * k)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun lerpP(a: List<Float>, b: List<Float>, t: Float) = P(lerp(a[0], b[0], t), lerp(a[1], b[1], t))
    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var d = (b - a) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return a + d * t
    }

    /** Двухзвенная обратная кинематика: возвращает сустав и конечную точку. */
    private fun ik(root: P, target: P, l1: Float, l2: Float, s: Int): Pair<P, P> {
        val dx = target.x - root.x
        val dy = target.y - root.y
        val dist = hypot(dx, dy).coerceIn(abs(l1 - l2) + 1e-4f, l1 + l2 - 1e-4f)
        val base = atan2(dy, dx)
        val cosA = ((l1 * l1 + dist * dist - l2 * l2) / (2 * l1 * dist)).coerceIn(-1f, 1f)
        val a = acos(cosA)
        val joint = P(root.x + l1 * cos(base + s * a), root.y + l1 * sin(base + s * a))
        val end = P(root.x + dist * cos(base), root.y + dist * sin(base))
        return joint to end
    }

    /** Поза в момент phase (0..1) цикла анимации. */
    fun pose(m: Move, phase: Float): Skeleton {
        val frames = m.frames
        if (frames.size == 1) return solve(m, frames[0], frames[0], 0f)
        val total = frames.sumOf { it.w.toDouble() }.toFloat()
        var t = (phase - kotlin.math.floor(phase)) * total
        var i = 0
        while (i < frames.lastIndex && t > frames[i].w) {
            t -= frames[i].w
            i++
        }
        val local = (t / frames[i].w).coerceIn(0f, 1f)
        val eased = (1f - cos(local * PI.toFloat())) / 2f
        return solve(m, frames[i], frames[(i + 1) % frames.size], eased)
    }

    /** Статичная поза для превью: «рабочий» кадр. */
    fun still(m: Move): Skeleton {
        val f = m.frames[if (m.frames.size > 1) 1 else 0]
        return solve(m, f, f, 0f)
    }

    private fun solve(m: Move, a: MoveFrame, b: MoveFrame, t: Float): Skeleton {
        val hip = lerpP(a.hip, b.hip, t)
        val torso = lerpAngle(a.torso, b.torso, t)
        val neckAng = lerpAngle(a.neck ?: a.torso, b.neck ?: b.torso, t)
        val bend = (if (t < 0.5f) a.bend else b.bend) ?: m.bend
        val shoulder = add(hip, dir(torso), TORSO)
        val shoulders: List<P>
        val hips: List<P>
        if (m.front) {
            val perp = dir(torso - 90f)
            shoulders = listOf(add(shoulder, perp, SHOULDER_HALF), add(shoulder, perp, -SHOULDER_HALF))
            hips = listOf(add(hip, perp, HIP_HALF), add(hip, perp, -HIP_HALF))
        } else {
            shoulders = listOf(shoulder, shoulder)
            hips = listOf(hip, hip)
        }
        val neck = add(shoulder, dir(neckAng), NECK)
        val head = add(neck, dir(neckAng), HEAD)
        val elbows = ArrayList<P>(2)
        val hands = ArrayList<P>(2)
        val knees = ArrayList<P>(2)
        val ankles = ArrayList<P>(2)
        val toes = ArrayList<P>(2)
        for (i in 0..1) {
            val (e, h) = ik(shoulders[i], lerpP(a.hands[i], b.hands[i], t), UPPER, FORE, bend[i])
            elbows += e
            hands += h
            val (k, an) = ik(hips[i], lerpP(a.feet[i], b.feet[i], t), THIGH, SHIN, bend[2 + i])
            knees += k
            ankles += an
            val rot = lerp(a.foot?.get(i) ?: 90f, b.foot?.get(i) ?: 90f, t)
            val shinAng = atan2(an.y - k.y, an.x - k.x) + rad(rot)
            toes += P(an.x + 0.07f * cos(shinAng), an.y + 0.07f * sin(shinAng))
        }
        return Skeleton(hip, shoulder, neck, head, shoulders, hips, elbows, hands, knees, ankles, toes)
    }
}
