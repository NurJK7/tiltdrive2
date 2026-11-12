package dev.tiltdrive.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

private val STEER_MODES = listOf("Tilt", "Wheel", "Arrows")

@Composable
fun App(vm: DriveViewModel) {
    val state by vm.link.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLang provides settings.lang) {
        Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (state == LinkState.Connected) DriveScreen(vm) else ConnectScreen(vm)
            }
        }
    }
}

@Composable
fun LangChips(lang: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("en" to "🇬🇧 EN", "ru" to "🇷🇺 RU").forEach { (code, label) ->
            val sel = code == lang
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.clip(shape)
                    .background(if (sel) Accent else Panel)
                    .border(1.dp, if (sel) Accent else Line, shape)
                    .clickable { onPick(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.Black else Color.White)
            }
        }
    }
}

// ───────────────────────────── Connect screen ─────────────────────────────

@Composable
fun ConnectScreen(vm: DriveViewModel) {
    val state by vm.link.state.collectAsStateWithLifecycle()
    val error by vm.link.error.collectAsStateWithLifecycle()
    val progress by vm.link.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val searching = state == LinkState.Searching
    val phoneIp = remember { vm.link.phoneIp() }

    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 44.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                Wheel(angle = -18f, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(8.dp))
                Text("TiltDrive", fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(tr("Use your phone as a steering wheel"), color = Dim, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Step("1", tr("Phone and PC on the same Wi-Fi"))
                Step("2", tr("In the game: Options → Controls → Hardware → “Use your phone or tablet” = Yes"))
                Step("3", tr("Tap AUTO-CONNECT. Tilt to steer, right half = gas, left half = brake"))
                Spacer(Modifier.height(14.dp))
                Text(tr("Dashboard (optional)"), color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    tr(
                        "In the game: Options → Advanced → Protocols → OutGauge. Address {0}, port 4445, rate 60. Then reload the car (Ctrl+R).",
                        phoneIp ?: tr("(join the Wi-Fi first)"),
                    ),
                    color = Color(0xFFC5CEDB), fontSize = 13.sp,
                )
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { vm.autoConnect() }, enabled = !searching,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(tr("AUTO-CONNECT"), fontSize = 18.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(10.dp))
                if (searching) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr("Looking for the game… {0}%", (progress * 100).roundToInt()),
                            color = Dim, fontSize = 13.sp, modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.link.disconnect() }) { Text(tr("Cancel")) }
                    }
                } else if (state == LinkState.Failed && error != null) {
                    val e = error ?: ""
                    Text(
                        if (e == ERR_NOT_FOUND) {
                            tr("Game not found. Same Wi-Fi? In the game set Options → Controls → Hardware → “Use your phone or tablet” to Yes, and allow BeamNG.drive in the Windows firewall.")
                        } else e,
                        color = BrakeCol, fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.link.listenOnly() }, enabled = !searching,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(tr("Dashboard only")) }
                Text(tr("Use this phone as a display, no pairing needed"), color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                Text(tr("Know the 5-digit code? Connect with it"), color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.code, onValueChange = { vm.code = it.filter(Char::isDigit).take(5) }, singleLine = true,
                    label = { Text(tr("Code")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.host, onValueChange = { vm.host = it }, singleLine = true,
                    label = { Text(tr("PC IP address (optional)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.connectManual() }, enabled = !searching && vm.code.length == 5,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(tr("Connect")) }
            }
        }
        LangChips(
            settings.lang,
            Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp),
        ) { vm.update(settings.copy(lang = it)) }
    }
}

@Composable
private fun Step(n: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(Accent),
            contentAlignment = Alignment.Center,
        ) { Text(n, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color(0xFFC5CEDB), fontSize = 14.sp)
    }
}

// ───────────────────────────── Drive screen ─────────────────────────────

@Composable
private fun noDataHint(rxInfo: String): String =
    if (rxInfo.isBlank()) {
        tr("Nothing received on :4445. In the game set OutGauge to this phone's IP, port 4445, then Ctrl+R.")
    } else tr("{0} · data not recognised", rxInfo)

@Composable
fun DriveScreen(vm: DriveViewModel) {
    val link = vm.link
    val ctx = LocalContext.current
    val view = LocalView.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tel by link.telemetry.collectAsStateWithLifecycle()
    val stale by link.stale.collectAsStateWithLifecycle()
    val rxInfo by link.rxInfo.collectAsStateWithLifecycle()
    val viewOnly by link.viewOnly.collectAsStateWithLifecycle()

    val tilt = remember {
        TiltSensor(ctx) { view.display?.rotation ?: android.view.Surface.ROTATION_90 }
    }
    DisposableEffect(tilt) {
        tilt.start()
        onDispose {
            tilt.stop()
            link.throttle = 0f
            link.brake = 0f
        }
    }

    var dashToggle by remember { mutableStateOf(false) }
    val dashOnly = viewOnly || dashToggle
    val dashNow by rememberUpdatedState(dashOnly)

    var trim by remember { mutableFloatStateOf(0f) }
    var wheelDeg by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }
    val cfg by rememberUpdatedState(settings)

    var gasDown by remember { mutableStateOf(false) }
    var brakeDown by remember { mutableStateOf(false) }
    var dragDeg by remember { mutableFloatStateOf(0f) }
    var touching by remember { mutableStateOf(false) }
    var arrowL by remember { mutableStateOf(false) }
    var arrowR by remember { mutableStateOf(false) }

    // One control loop (~60 Hz): steering from the chosen mode, pedals on/off.
    LaunchedEffect(tilt) {
        var smooth = 0f
        var arrowVal = 0f
        while (isActive) {
            if (dashNow) {
                link.steerWire = 0.5f
                link.throttle = 0f
                link.brake = 0f
                delay(50)
                continue
            }
            val s = cfg
            val steer: Float = when (s.steerMode) {
                1 -> {
                    if (!touching) {
                        dragDeg *= 0.8f
                        if (abs(dragDeg) < 0.3f) dragDeg = 0f
                    }
                    (dragDeg / s.range).coerceIn(-1f, 1f)
                }
                2 -> {
                    val tgt = if (arrowR && !arrowL) 1f else if (arrowL && !arrowR) -1f else 0f
                    val rate = if (tgt == 0f) 0.10f else 0.05f
                    arrowVal += (tgt - arrowVal).coerceIn(-rate, rate)
                    arrowVal
                }
                else -> {
                    val n = ((tilt.angleDeg - trim) / s.range).coerceIn(-1f, 1f)
                    val dz = s.deadzone / 100f
                    val v = if (abs(n) <= dz) 0f else sign(n) * (abs(n) - dz) / (1f - dz)
                    smooth += (1f - s.smoothing / 100f * 0.92f) * (v - smooth)
                    sign(smooth) * abs(smooth).pow(1.3f) // gentle near center, full at the edges
                }
            }
            val right = if (s.invert) -steer else steer      // +1 = full right
            link.steerWire = 0.5f - 0.5f * right             // wire: 0 = right, 1 = left, 0.5 = straight
            wheelDeg = steer * s.range
            link.throttle = if (gasDown) 1f else 0f
            link.brake = if (brakeDown) 1f else 0f
            delay(16)
        }
    }

    val cycleStyle = { vm.update(settings.copy(dashStyle = (settings.dashStyle + 1) % DASH_STYLES.size)) }
    val dashTap = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
    ) { cycleStyle() }

    if (dashOnly) {
        Box(Modifier.fillMaxSize()) {
            Dashboard(
                tel, settings.dashStyle, settings.mph,
                Modifier.fillMaxSize().padding(top = 40.dp, bottom = 4.dp).then(dashTap),
            )
            Row(Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                if (!viewOnly) TextButton(onClick = { dashToggle = false }) { Text(tr("Controls")) }
                TextButton(onClick = { showSettings = true }) { Text(tr("Settings")) }
                TextButton(onClick = { link.disconnect() }) { Text(tr("Exit"), color = BrakeCol) }
            }
            if (tel == null) {
                Text(
                    noDataHint(rxInfo), color = Dim, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 10.dp, start = 12.dp, end = 220.dp),
                )
            }
        }
    } else {
        val wheelSize = if (settings.steerMode == 1) 175.dp else 140.dp
        val wheelTouch = if (settings.steerMode == 1) {
            Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    fun ang(p: Offset) = Math.toDegrees(atan2((p.y - c.y).toDouble(), (p.x - c.x).toDouble())).toFloat()
                    var prev = ang(down.position)
                    var total = 0f
                    touching = true
                    dragDeg = 0f
                    var tracking = true
                    while (tracking) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch == null || !ch.pressed) {
                            tracking = false
                        } else {
                            val a = ang(ch.position)
                            var d = a - prev
                            if (d > 180f) d -= 360f
                            if (d < -180f) d += 360f
                            total += d
                            prev = a
                            dragDeg = total
                            ch.consume()
                        }
                    }
                    touching = false
                }
            }
        } else Modifier

        Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pedal(tr("BRAKE"), BrakeCol, Modifier.weight(0.85f).fillMaxHeight()) { brakeDown = it }
            Column(Modifier.weight(2.3f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val (txt, col) = when {
                        tel == null -> tr("Connected · no dashboard data") to Accent
                        stale -> tr("No telemetry from the game") to Accent
                        else -> tr("Connected") to Gas
                    }
                    Text(
                        txt, color = col, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.update(settings.copy(steerMode = (settings.steerMode + 1) % 3)) }) {
                        Text(tr(STEER_MODES[settings.steerMode]))
                    }
                    if (settings.steerMode == 0) {
                        TextButton(onClick = { trim = tilt.angleDeg }) { Text(tr("Center")) }
                    }
                    TextButton(onClick = { dashToggle = true }) { Text(tr("Dash")) }
                    TextButton(onClick = { showSettings = true }) { Text(tr("Settings")) }
                    TextButton(onClick = { link.disconnect() }) { Text(tr("Exit"), color = BrakeCol) }
                }
                if (tel == null) {
                    Text(noDataHint(rxInfo), color = Dim, fontSize = 11.sp)
                }
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Wheel(angle = wheelDeg, modifier = Modifier.size(wheelSize).then(wheelTouch))
                    Dashboard(tel, settings.dashStyle, settings.mph, Modifier.weight(1f).fillMaxHeight().then(dashTap))
                }
                if (settings.steerMode == 2) {
                    Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HoldButton("◀", Modifier.weight(1f).fillMaxHeight()) { arrowL = it }
                        HoldButton("▶", Modifier.weight(1f).fillMaxHeight()) { arrowR = it }
                    }
                }
            }
            Pedal(tr("GAS"), Gas, Modifier.weight(0.85f).fillMaxHeight()) { gasDown = it }
        }
    }

    if (showSettings) {
        SettingsDialog(settings, vm::update) { showSettings = false }
    }
}

@Composable
private fun Pedal(label: String, color: Color, modifier: Modifier, onDown: (Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    val cb by rememberUpdatedState(onDown)
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (down) color.copy(alpha = 0.28f) else Panel)
            .border(2.dp, if (down) color else Line, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    down = true
                    cb(true)
                    var tracking = true
                    while (tracking) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == d.id }
                        if (ch == null || !ch.pressed) tracking = false else ch.consume()
                    }
                    down = false
                    cb(false)
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val n = 9
            for (i in 1 until n) {
                val y = size.height * i / n
                drawLine(Color(0x22FFFFFF), Offset(size.width * 0.15f, y), Offset(size.width * 0.85f, y), strokeWidth = 4f)
            }
        }
        Text(
            label, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun HoldButton(label: String, modifier: Modifier, onHold: (Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    val hold by rememberUpdatedState(onHold)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (down) Accent.copy(alpha = 0.35f) else Panel)
            .border(2.dp, if (down) Accent else Line, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    down = true
                    hold(true)
                    var tracking = true
                    while (tracking) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == d.id }
                        if (ch == null || !ch.pressed) tracking = false else ch.consume()
                    }
                    down = false
                    hold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 30.sp, color = Accent, fontWeight = FontWeight.Black) }
}

@Composable
fun Wheel(angle: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.9f
        rotate(angle, pivot = c) {
            drawCircle(Accent, r, c, style = Stroke(width = r * 0.16f))
            drawLine(Accent, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = r * 0.13f, cap = StrokeCap.Round)
            drawLine(Accent, c, Offset(c.x, c.y + r), strokeWidth = r * 0.13f, cap = StrokeCap.Round)
            drawCircle(Accent, r * 0.22f, c)
            drawLine(Color.White, Offset(c.x, c.y - r * 1.04f), Offset(c.x, c.y - r * 0.7f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        }
    }
}

// ───────────────────────────── Settings (BeamNG style) ─────────────────────────────

private val SideBg = Color(0xFF0E1013)
private val SectionBg = Color(0xFF1D2128)
private val RowBg = Color(0xFF282D37)

/** Closes only with the Done button: no dismiss on outside tap or Back. */
@Composable
private fun SettingsDialog(s: Settings, onChange: (Settings) -> Unit, onClose: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf("Steering", "Dashboard", "Language", "About")

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            Modifier.fillMaxSize().padding(10.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xF2121519),
            contentColor = Color.White,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Accent)
                            .clickable(onClick = onClose).padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(tr("‹ Done"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                    Spacer(Modifier.width(16.dp))
                    Canvas(Modifier.size(16.dp, 32.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.45f, 0f); lineTo(size.width, 0f)
                            lineTo(size.width * 0.55f, size.height); lineTo(0f, size.height); close()
                        }
                        drawPath(path, Accent)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Settings"), fontSize = 26.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.width(170.dp).fillMaxHeight().background(SideBg)) {
                        pages.forEachIndexed { i, name ->
                            val sel = i == page
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(if (sel) Accent else Color.Transparent)
                                    .clickable { page = i }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                            ) {
                                Text(tr(name), fontSize = 15.sp, color = Color.White,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("TiltDrive v0.2", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp),
                    ) {
                        when (page) {
                            0 -> {
                                Section(tr("Control")) {
                                    SegmentRow(tr("Steering control"), STEER_MODES.map { tr(it) }, s.steerMode) { onChange(s.copy(steerMode = it)) }
                                    ToggleRow(tr("Reverse steering"), s.invert) { onChange(s.copy(invert = it)) }
                                }
                                Section(tr("Sensitivity")) {
                                    SliderRow(tr("Steering range"), "${s.range.roundToInt()}°", s.range, 30f..120f) { onChange(s.copy(range = it)) }
                                    SliderRow(tr("Dead zone (tilt)"), "${s.deadzone.roundToInt()}%", s.deadzone, 0f..15f) { onChange(s.copy(deadzone = it)) }
                                    SliderRow(tr("Smoothing (tilt)"), "${s.smoothing.roundToInt()}%", s.smoothing, 0f..90f) { onChange(s.copy(smoothing = it)) }
                                }
                            }
                            1 -> {
                                Section(tr("Dashboard style")) {
                                    SegmentRow(tr("Style"), DASH_STYLES.map { tr(it) }, s.dashStyle) { onChange(s.copy(dashStyle = it)) }
                                    Text(tr("Tap the dashboard while driving to switch the style"), color = Dim, fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                                }
                                Section(tr("Units")) {
                                    ToggleRow(tr("Speed in mph"), s.mph) { onChange(s.copy(mph = it)) }
                                }
                            }
                            2 -> Section(tr("Language")) {
                                LangChips(s.lang, Modifier.padding(4.dp)) { onChange(s.copy(lang = it)) }
                            }
                            else -> Section(tr("About")) {
                                Text(
                                    tr("Unofficial app, not affiliated with BeamNG. Protocol info from github.com/BeamNG/remotecontrol (ISC license, © 2014 BeamNG)."),
                                    color = Dim, fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp)).background(SectionBg).padding(10.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
private fun RowBox(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp)).background(RowBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    RowBox {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked, onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Accent, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color(0xFF3A404C), uncheckedThumbColor = Color(0xFFB8C0CC),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SliderRow(
    label: String, valueText: String, value: Float,
    range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp)).background(RowBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Slider(value, onChange, valueRange = range)
    }
}

@Composable
private fun SegmentRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    RowBox {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF14171C))) {
            options.forEachIndexed { i, name ->
                val sel = i == selected
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (sel) Accent else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) Color.Black else Color(0xFFC5CEDB))
                }
            }
        }
    }
}
