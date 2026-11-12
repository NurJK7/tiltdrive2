package dev.tiltdrive.remote

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow

data class Settings(
    val range: Float = 70f,       // degrees for full lock (tilt and on-screen wheel)
    val deadzone: Float = 3f,     // percent
    val invert: Boolean = false,  // one switch, same meaning in every steering mode
    val mph: Boolean = false,
    val steerMode: Int = 0,       // 0 tilt, 1 on-screen wheel, 2 arrows
    val smoothing: Float = 40f,   // percent, tilt steering
    val dashStyle: Int = 0,       // see DASH_STYLES
    val lang: String = "en",
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tiltdrive", Context.MODE_PRIVATE)
    var lastCode: String
        get() = sp.getString("code", "") ?: ""
        set(v) = sp.edit().putString("code", v).apply()
    var lastHost: String
        get() = sp.getString("host", "") ?: ""
        set(v) = sp.edit().putString("host", v).apply()

    // "invert2": the tilt direction was fixed, so the old "invert" value no longer applies.
    fun load() = Settings(
        range = sp.getFloat("range", 70f),
        deadzone = sp.getFloat("deadzone", 3f),
        invert = sp.getBoolean("invert2", false),
        mph = sp.getBoolean("mph", false),
        steerMode = sp.getInt("steerMode", 0),
        smoothing = sp.getFloat("smoothing", 40f),
        dashStyle = sp.getInt("dashStyle", 0),
        lang = sp.getString("lang", null)
            ?: if (java.util.Locale.getDefault().language == "ru") "ru" else "en",
    )

    fun save(s: Settings) = sp.edit()
        .putFloat("range", s.range).putFloat("deadzone", s.deadzone)
        .putBoolean("invert2", s.invert).putBoolean("mph", s.mph)
        .putInt("steerMode", s.steerMode).putFloat("smoothing", s.smoothing)
        .putInt("dashStyle", s.dashStyle).putString("lang", s.lang)
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
