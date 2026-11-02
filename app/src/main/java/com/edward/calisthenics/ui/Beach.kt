package com.edward.calisthenics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.time.LocalTime

/** Время суток для фона: утро — рассвет, вечер — закат, днём — яркое небо. */
enum class SkyMode { DAWN, DAY, SUNSET }

fun skyNow(t: LocalTime = LocalTime.now()): SkyMode = when (t.hour) {
    in 4..9 -> SkyMode.DAWN
    in 10..15 -> SkyMode.DAY
    else -> SkyMode.SUNSET
}

/** Нарисованный пляж Муйне: небо, солнце над морем, волна, песок и пальма. */
@Composable
fun BeachBackground(modifier: Modifier = Modifier, mode: SkyMode = skyNow()) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sky = when (mode) {
            SkyMode.DAWN -> listOf(Color(0xFF5AA9E6), Color(0xFF9FD3F0), Color(0xFFFFD9A8))
            SkyMode.DAY -> listOf(Color(0xFF3D9BE0), Color(0xFF7CC4EE), Color(0xFFCDEBF7))
            SkyMode.SUNSET -> listOf(Color(0xFF2B2A5C), Color(0xFFA4508B), Color(0xFFFF8A5C))
        }
        val horizon = h * 0.52f
        drawRect(Brush.verticalGradient(sky, startY = 0f, endY = horizon))
        val sunset = mode == SkyMode.SUNSET
        val sunX = if (sunset) w * 0.3f else w * 0.72f
        val sunY = when (mode) {
            SkyMode.DAWN -> horizon - h * 0.02f
            SkyMode.DAY -> h * 0.16f
            SkyMode.SUNSET -> horizon
        }
        val sunR = h * if (sunset) 0.085f else 0.07f
        drawCircle(Color(if (sunset) 0x55FFD27A else 0x55FFF3C4), sunR * 1.6f, Offset(sunX, sunY))
        drawCircle(Color(if (sunset) 0xFFFFD27A else 0xFFFFF3C4), sunR, Offset(sunX, sunY))
        // Море.
        val sea = if (sunset) listOf(Color(0xFFB0607A), Color(0xFF5A3A78)) else listOf(Color(0xFF4FB3DE), Color(0xFF1F6F9C))
        drawRect(Brush.verticalGradient(sea, startY = horizon, endY = h * 0.72f), topLeft = Offset(0f, horizon), size = Size(w, h * 0.22f))
        if (mode != SkyMode.DAY) {
            val glow = Color(if (sunset) 0xBBFFCF8A else 0xBBFFF6D5)
            for ((i, k) in listOf(0.16f, 0.1f, 0.05f).withIndex()) {
                val y = horizon + h * (0.012f + 0.022f * i)
                drawLine(glow, Offset(sunX - w * k, y), Offset(sunX + w * k, y), strokeWidth = h * 0.007f, cap = StrokeCap.Round)
            }
        }
        // Пена и песок.
        val foam = Path().apply {
            moveTo(0f, h * 0.70f)
            quadraticBezierTo(w * 0.16f, h * 0.675f, w * 0.32f, h * 0.70f)
            quadraticBezierTo(w * 0.5f, h * 0.72f, w * 0.66f, h * 0.695f)
            quadraticBezierTo(w * 0.84f, h * 0.675f, w, h * 0.70f)
            lineTo(w, h * 0.74f); lineTo(0f, h * 0.74f); close()
        }
        drawPath(foam, Color(if (sunset) 0xD9E9A07A else 0xD9BFE7F2))
        val sand = if (sunset) listOf(Color(0xFFD79A6A), Color(0xFFC4865A)) else listOf(Color(0xFFF0D6A6), Color(0xFFE2C08A))
        val beach = Path().apply {
            moveTo(0f, h * 0.715f)
            quadraticBezierTo(w * 0.25f, h * 0.70f, w * 0.5f, h * 0.715f)
            quadraticBezierTo(w * 0.75f, h * 0.73f, w, h * 0.71f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(beach, Brush.verticalGradient(sand, startY = h * 0.7f, endY = h))
        // Пальма.
        val px = if (sunset) w * 0.88f else w * 0.1f
        val trunk = Color(if (sunset) 0xFF3A2230 else 0xFF7A5A3A)
        val leaves = Color(if (sunset) 0xFF2A1830 else 0xFF2F7A4A)
        val base = Offset(px, h * 0.75f)
        val top = Offset(px - w * 0.01f, h * 0.43f)
        drawPath(
            Path().apply { moveTo(base.x, base.y); quadraticBezierTo(px + w * 0.02f, h * 0.6f, top.x, top.y) },
            trunk, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round),
        )
        val leafs = listOf(-0.16f to 0.05f, 0.17f to 0.03f, -0.13f to -0.08f, 0.12f to -0.09f, 0.0f to -0.1f)
        for ((dx, dy) in leafs) {
            val tip = Offset(top.x + w * dx, top.y + h * (0.05f + dy * -0.4f) + h * 0.04f)
            val ctrl = Offset(top.x + w * dx * 0.5f, top.y - h * 0.05f)
            drawPath(
                Path().apply {
                    moveTo(top.x, top.y)
                    quadraticBezierTo(ctrl.x, ctrl.y, tip.x, tip.y)
                    quadraticBezierTo(ctrl.x, ctrl.y + h * 0.03f, top.x, top.y)
                    close()
                },
                leaves,
            )
        }
    }
}
