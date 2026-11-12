package dev.tiltdrive.remote

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val DASH_STYLES = listOf("M Sport", "Race", "EV", "Classic")

private class Scale {
    var rpm = 4000f
    var speed = 0f
}

/** Everything a style needs, already prepared. */
private class D(
    val has: Boolean,
    val speed: Float,
    val speedStr: String,
    val rpm: Float,
    val frac: Float,
    val maxRpm: Float,
    val gear: String,
    val fuel: Float,
    val temp: Float,
    val unit: String,
    val over: Boolean,
    val blink: Boolean,
    val lowFuel: Boolean,
    val hot: Boolean,
    val speedMax: Float,
    val speedStep: Float,
)

@Composable
fun Dashboard(t: Telemetry?, style: Int, mph: Boolean, modifier: Modifier = Modifier) {
    val sc = remember { Scale() }
    val sp = t?.speed(mph) ?: 0f
    if (t != null) {
        if (t.rpm > sc.rpm) sc.rpm = t.rpm
        if (sp > sc.speed) sc.speed = sp
    }
    val maxRpm = (ceil(sc.rpm * 1.04f / 1000f) * 1000f).coerceAtLeast(4000f)
    val want = max(sc.speed * 1.1f, if (mph) 100f else 160f)
    val step = if (want > 200f) 40f else 20f
    val speedMax = ceil(want / step) * step

    val inf = rememberInfiniteTransition(label = "blink")
    val phase by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(280), RepeatMode.Reverse), label = "phase",
    )
    val blink = phase > 0.5f

    val rpm = t?.rpm ?: 0f
    val frac = (rpm / maxRpm).coerceIn(0f, 1f)
    val fuel = (t?.fuel ?: 0f).coerceIn(0f, 1f)
    val temp = t?.engTemp ?: 0f
    val d = D(
        has = t != null,
        speed = sp,
        speedStr = if (t != null) sp.roundToInt().toString() else "--",
        rpm = rpm, frac = frac, maxRpm = maxRpm,
        gear = t?.gearLabel ?: "N",
        fuel = fuel, temp = temp,
        unit = tr(if (mph) "mph" else "km/h"),
        over = t != null && (frac > 0.93f || t.lamp(Lights.SHIFT)),
        blink = blink,
        lowFuel = t != null && fuel < 0.10f,
        hot = t != null && temp > 110f,
        speedMax = speedMax, speedStep = step,
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            when (style) {
                0 -> mSport(d)
                1 -> race(d)
                2 -> ev(d)
                else -> classic(d)
            }
        }
        Lamps(t, d)
    }
}

// ───────────────────────────── drawing helpers ─────────────────────────────

private fun pt(c: Offset, r: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
}

/** Text centered on (x, y). */
private fun DrawScope.text(
    s: String, x: Float, y: Float, size: Float, color: Color,
    bold: Boolean = true, mono: Boolean = false,
) {
    val p = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = size
        this.color = color.toArgb()
        typeface = Typeface.create(
            if (mono) Typeface.MONOSPACE else Typeface.DEFAULT,
            if (bold) Typeface.BOLD else Typeface.NORMAL,
        )
    }
    drawContext.canvas.nativeCanvas.drawText(s, x, y + size * 0.35f, p)
}

private val TrackCol = Color(0xFF1B222C)
private val TickCol = Color(0xFF8FA3B8)
private val RedCol = Color(0xFFE7222E)

private fun DrawScope.hbar(x: Float, y: Float, w: Float, h: Float, frac: Float, color: Color) {
    drawRoundRect(TrackCol, Offset(x, y), Size(w, h), CornerRadius(h / 2f))
    if (frac > 0f) drawRoundRect(color, Offset(x, y), Size(w * frac.coerceIn(0f, 1f), h), CornerRadius(h / 2f))
}

private fun DrawScope.vbar(cx: Float, cy: Float, h: Float, w: Float, frac: Float, color: Color, label: String, lsize: Float) {
    val top = cy - h / 2f
    drawRoundRect(TrackCol, Offset(cx - w / 2f, top), Size(w, h), CornerRadius(w / 2f))
    val fh = h * frac.coerceIn(0f, 1f)
    if (fh > 0f) drawRoundRect(color, Offset(cx - w / 2f, top + h - fh), Size(w, fh), CornerRadius(w / 2f))
    text(label, cx, top - lsize, lsize, Dim)
}

// ───────────────────────────── style 0: M Sport ─────────────────────────────

private fun DrawScope.mSport(d: D) {
    val r = min(size.width, size.height) * 0.47f
    val c = Offset(size.width / 2f, size.height / 2f)
    val stroke = r * 0.13f
    val ar = r - stroke / 2f
    val tl = Offset(c.x - ar, c.y - ar)
    val sz = Size(ar * 2f, ar * 2f)
    val blue1 = Color(0xFF81C4FF)
    val blue2 = Color(0xFF1C69D4)

    drawArc(TrackCol, 135f, 270f, false, tl, sz, style = Stroke(stroke))
    val zoneFrom = floatArrayOf(0f, 0.6f, 0.85f)
    val zoneTo = floatArrayOf(0.6f, 0.85f, 1f)
    val cols = listOf(blue1, blue2, RedCol)
    for (i in 0..2) {
        val end = min(d.frac, zoneTo[i])
        if (end > zoneFrom[i]) {
            val col = if (d.over && d.blink) Color.White else cols[i]
            drawArc(col, 135f + 270f * zoneFrom[i], 270f * (end - zoneFrom[i]), false, tl, sz, style = Stroke(stroke))
        }
    }
    val n = (d.maxRpm / 1000f).roundToInt()
    for (k in 0..n) {
        val f = k * 1000f / d.maxRpm
        val deg = 135f + 270f * f
        val col = if (f >= 0.85f) RedCol else TickCol
        drawLine(col, pt(c, r - stroke * 1.25f, deg), pt(c, r - stroke * 1.9f, deg), strokeWidth = max(2f, r * 0.012f))
        text(k.toString(), pt(c, r - stroke * 3.0f, deg).x, pt(c, r - stroke * 3.0f, deg).y, r * 0.11f, col, bold = false)
    }
    text(d.gear, c.x, c.y - r * 0.2f, r * 0.55f, if (d.over && d.blink) RedCol else Color.White)
    text(d.speedStr, c.x, c.y + r * 0.30f, r * 0.30f, Color.White, mono = true)
    text(d.unit, c.x, c.y + r * 0.50f, r * 0.09f, Dim)
    val bw = r * 0.14f
    val by = c.y + r * 0.64f
    drawRect(blue1, Offset(c.x - bw * 1.6f, by), Size(bw, bw * 0.35f))
    drawRect(blue2, Offset(c.x - bw * 0.5f, by), Size(bw, bw * 0.35f))
    drawRect(RedCol, Offset(c.x + bw * 0.6f, by), Size(bw, bw * 0.35f))

    val bx = max(r * 0.12f, c.x - r * 1.28f)
    val bh = r * 1.1f
    vbar(bx, c.y, bh, r * 0.07f, d.fuel, if (d.lowFuel && d.blink) RedCol else blue1, "F", r * 0.09f)
    vbar(size.width - bx, c.y, bh, r * 0.07f, (d.temp / 130f), if (d.hot) RedCol else blue2, "T", r * 0.09f)
}

// ───────────────────────────── style 1: Race ─────────────────────────────

private fun DrawScope.race(d: D) {
    val w = size.width
    val h = size.height
    val leds = 15
    val gap = w * 0.008f
    val x0 = w * 0.03f
    val lw = (w - x0 * 2f - gap * (leds - 1)) / leds
    val lh = h * 0.12f
    val y0 = h * 0.04f
    val p = ((d.frac - 0.45f) / 0.55f).coerceIn(0f, 1f)
    for (i in 0 until leds) {
        val base = when {
            i < 5 -> Color(0xFF22D36B)
            i < 10 -> Color(0xFFFFC21A)
            else -> Color(0xFFFF3B30)
        }
        val lit = if (d.over) d.blink else p * leds > i
        drawRoundRect(
            if (lit) base else TrackCol,
            Offset(x0 + i * (lw + gap), y0), Size(lw, lh), CornerRadius(lw * 0.3f),
        )
    }
    text(d.gear, w * 0.22f, h * 0.52f, h * 0.62f, if (d.over && d.blink) RedCol else Accent)
    text(d.speedStr, w * 0.64f, h * 0.48f, h * 0.44f, Color.White, mono = true)
    text(d.unit, w * 0.64f, h * 0.73f, h * 0.08f, Dim)
    text("${d.rpm.roundToInt()} rpm", w * 0.5f, h * 0.84f, h * 0.07f, Dim, bold = false)
    val by = h * 0.92f
    val bh = h * 0.035f
    hbar(w * 0.04f, by, w * 0.40f, bh, d.fuel, if (d.lowFuel && d.blink) RedCol else Color(0xFF22D36B))
    hbar(w * 0.56f, by, w * 0.40f, bh, d.temp / 130f, if (d.hot) RedCol else Color(0xFFFFC21A))
}

// ───────────────────────────── style 2: EV ─────────────────────────────

private fun DrawScope.ev(d: D) {
    val cyan = Color(0xFF4FC3F7)
    val w = size.width
    val h = size.height
    val r = min(w, h * 0.92f) * 0.46f
    val c = Offset(w / 2f, h * 0.47f)
    val stroke = r * 0.05f
    val ar = r - stroke
    val tl = Offset(c.x - ar, c.y - ar)
    val sz = Size(ar * 2f, ar * 2f)
    drawArc(Color(0xFF1A2230), 135f, 270f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
    val f = (d.speed / d.speedMax).coerceIn(0f, 1f)
    if (f > 0.005f) {
        drawArc(
            if (f > 0.85f) Color.White else cyan, 135f, 270f * f, false, tl, sz,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    text(d.speedStr, c.x, c.y - r * 0.05f, r * 0.62f, Color.White, bold = false)
    text(d.unit, c.x, c.y + r * 0.33f, r * 0.10f, Dim)
    val g = if (d.gear == "R" || d.gear == "N") d.gear else "D"
    text(g, c.x, c.y + r * 0.58f, r * 0.15f, cyan)

    // battery-style fuel gauge, bottom left
    val bw = w * 0.20f
    val bh = h * 0.075f
    val bx = w * 0.05f
    val by = h * 0.86f
    drawRoundRect(Color(0xFF3A4556), Offset(bx, by), Size(bw, bh), CornerRadius(bh * 0.25f), style = Stroke(2f))
    val fc = if (d.lowFuel && d.blink) RedCol else cyan
    if (d.fuel > 0f) drawRoundRect(fc, Offset(bx + 3f, by + 3f), Size((bw - 6f) * d.fuel, bh - 6f), CornerRadius(bh * 0.2f))
    text("${(d.fuel * 100f).roundToInt()}%", bx + bw + w * 0.07f, by + bh / 2f, h * 0.06f, Dim)
    text("${d.temp.roundToInt()}°C", w * 0.88f, by + bh / 2f, h * 0.06f, if (d.hot) RedCol else Dim)
    hbar(w * 0.30f, h * 0.955f, w * 0.40f, h * 0.014f, d.frac, cyan)
}

// ───────────────────────────── style 3: Classic ─────────────────────────────

private fun DrawScope.dial(c: Offset, r: Float, frac: Float, labels: List<String>, redFrom: Float, small: String) {
    drawCircle(Color(0xFF0F141B), r, c)
    drawCircle(Color(0xFF2A3342), r, c, style = Stroke(r * 0.04f))
    if (redFrom < 1f) {
        val ar = r * 0.9f
        drawArc(
            RedCol, 135f + 270f * redFrom, 270f * (1f - redFrom), false,
            Offset(c.x - ar, c.y - ar), Size(ar * 2f, ar * 2f), style = Stroke(r * 0.03f),
        )
    }
    val last = max(1, labels.size - 1)
    labels.forEachIndexed { i, s ->
        val f = i / last.toFloat()
        val deg = 135f + 270f * f
        drawLine(TickCol, pt(c, r * 0.80f, deg), pt(c, r * 0.90f, deg), strokeWidth = max(2f, r * 0.018f))
        val p = pt(c, r * 0.66f, deg)
        text(s, p.x, p.y, r * 0.12f, Color.White, bold = false)
    }
    text(small, c.x, c.y + r * 0.40f, r * 0.10f, Dim)
    val deg = 135f + 270f * frac.coerceIn(0f, 1f)
    drawLine(Accent, c, pt(c, r * 0.78f, deg), strokeWidth = max(3f, r * 0.04f), cap = StrokeCap.Round)
    drawCircle(Accent, r * 0.07f, c)
}

private fun DrawScope.classic(d: D) {
    val w = size.width
    val h = size.height
    val r = min(h * 0.47f, w * 0.27f)
    val cl = Offset(w * 0.25f, h * 0.5f)
    val cr = Offset(w * 0.75f, h * 0.5f)
    val sl = (0..(d.speedMax / d.speedStep).roundToInt()).map { (it * d.speedStep).roundToInt().toString() }
    dial(cl, r, d.speed / d.speedMax, sl, 2f, d.unit)
    val tl = (0..(d.maxRpm / 1000f).roundToInt()).map { it.toString() }
    dial(cr, r, d.frac, tl, 0.85f, "x1000 rpm")
    val cx = w * 0.5f
    text(d.gear, cx, h * 0.34f, r * 0.55f, if (d.over && d.blink) RedCol else Accent)
    text(d.speedStr, cx, h * 0.58f, r * 0.26f, Color.White, mono = true)
    val bw = w * 0.12f
    hbar(cx - bw / 2f, h * 0.72f, bw, h * 0.03f, d.fuel, if (d.lowFuel && d.blink) RedCol else Gas)
    text("F", cx - bw / 2f - w * 0.012f, h * 0.735f, h * 0.05f, Dim)
    hbar(cx - bw / 2f, h * 0.80f, bw, h * 0.03f, d.temp / 130f, if (d.hot) RedCol else Color(0xFFFFC21A))
    text("T", cx - bw / 2f - w * 0.012f, h * 0.815f, h * 0.05f, Dim)
}

// ───────────────────────────── warning lamps ─────────────────────────────

@Composable
private fun Lamps(t: Telemetry?, d: D) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lamp("◀", t?.lamp(Lights.SIGNAL_L) == true, Gas)
        Lamp("▶", t?.lamp(Lights.SIGNAL_R) == true, Gas)
        Lamp("HI", t?.lamp(Lights.FULLBEAM) == true, Color(0xFF4DA3FF))
        Lamp("ABS", t?.lamp(Lights.ABS) == true, Accent)
        Lamp("TC", t?.lamp(Lights.TC) == true, Accent)
        Lamp("P", t?.lamp(Lights.HANDBRAKE) == true, BrakeCol)
        Lamp("OIL", t?.lamp(Lights.OIL) == true, BrakeCol)
        Lamp("BAT", t?.lamp(Lights.BATTERY) == true, BrakeCol)
        Lamp(tr("FUEL"), d.lowFuel && d.blink, Accent)
        Lamp(tr("TEMP"), d.hot && d.blink, BrakeCol)
    }
}

@Composable
private fun Lamp(text: String, on: Boolean, color: Color) {
    Text(text, color = if (on) color else Color(0xFF2A3547), fontWeight = FontWeight.Bold, fontSize = 14.sp)
}
