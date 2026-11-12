package dev.tiltdrive.remote

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Bg = Color(0xFF0A0E14)
internal val Panel = Color(0xFF131A24)
internal val Line = Color(0xFF223044)
internal val Accent = Color(0xFFFF7A1A)
internal val Gas = Color(0xFF1FD6A0)
internal val BrakeCol = Color(0xFFFF4D5A)
internal val Dim = Color(0xFF7C8A9E)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Color.Black,
            background = Bg, surface = Panel, onSurface = Color.White, onBackground = Color.White,
        ),
        content = content,
    )
}
