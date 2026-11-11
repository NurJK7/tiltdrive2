package dev.tiltdrive.remote

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow

data class Settings(
    val range: Float = 70f,       // degrees of tilt for full lock
    val deadzone: Float = 3f,     // percent
    val invert: Boolean = false,
    val analog: Boolean = false,  // pedals: slide up/down to modulate
    val haptics: Boolean = true,
    val mph: Boolean = false,
    val steerMode: Int = 0,       // 0 tilt, 1 on-screen wheel, 2 arrows
    val smoothing: Float = 40f,   // percent, tilt steering
    val gasLimit: Float = 100f,   // percent, max throttle when the pedal is pressed
    val gasRampMs: Float = 0f,    // time to reach the limit, 0 = instant
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tiltdrive", Context.MODE_PRIVATE)
    var lastCode: String
        get() = sp.getString("code", "") ?: ""
        set(v) = sp.edit().putString("code", v).apply()
    var lastHost: String
        get() = sp.getString("host", "") ?: ""
        set(v) = sp.edit().putString("host", v).apply()

    fun load() = Settings(
        range = sp.getFloat("range", 70f),
        deadzone = sp.getFloat("deadzone", 3f),
        invert = sp.getBoolean("invert", false),
        analog = sp.getBoolean("analog", false),
        haptics = sp.getBoolean("haptics", true),
        mph = sp.getBoolean("mph", false),
        steerMode = sp.getInt("steerMode", 0),
        smoothing = sp.getFloat("smoothing", 40f),
        gasLimit = sp.getFloat("gasLimit", 100f),
        gasRampMs = sp.getFloat("gasRampMs", 0f),
    )

    fun save(s: Settings) = sp.edit()
        .putFloat("range", s.range).putFloat("deadzone", s.deadzone)
        .putBoolean("invert", s.invert).putBoolean("analog", s.analog)
        .putBoolean("haptics", s.haptics).putBoolean("mph", s.mph)
        .putInt("steerMode", s.steerMode).putFloat("smoothing", s.smoothing)
        .putFloat("gasLimit", s.gasLimit).putFloat("gasRampMs", s.gasRampMs)
        .apply()
}

class DriveViewModel(app: Application) : AndroidViewModel(app) {
    val link = BeamLink(app)
    private val prefs = Prefs(app)
    val settings = MutableStateFlow(prefs.load())
    var code by mutableStateOf(prefs.lastCode)
    var host by mutableStateOf(prefs.lastHost)

    init {
        link.onCodeFound = { c ->
            code = c
            prefs.lastCode = c
        }
    }

    fun update(s: Settings) {
        settings.value = s
        prefs.save(s)
    }

    /** No QR, no typing: tries the saved code, then finds the game on the Wi-Fi by itself. */
    fun autoConnect() {
        prefs.lastHost = host
        link.connect(null, code.ifBlank { null }, host.ifBlank { null })
    }

    fun connectManual() {
        prefs.lastCode = code
        prefs.lastHost = host
        link.connect(null, code.trim(), host.ifBlank { null })  // try this code, else search
    }

    override fun onCleared() {
        link.disconnect()
    }
}
