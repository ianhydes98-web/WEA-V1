package com.wae.statussaver.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WaeColors = darkColorScheme(
    primary = Color(0xFF25D366),
    onPrimary = Color(0xFF07230F),
    primaryContainer = Color(0xFF1DA851),
    onPrimaryContainer = Color(0xFF07230F),
    secondary = Color(0xFF9A9CA3),
    onSecondary = Color(0xFF07230F),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF16171A),
    onSurface = Color(0xFFEDEDEF),
    surfaceVariant = Color(0xFF1E1F23),
    onSurfaceVariant = Color(0xFF9A9CA3),
    outline = Color(0xFF2A2B30),
    error = Color(0xFFE5534B),
    onError = Color(0xFF2A0A08)
)

@Composable
fun WaeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = WaeColors, content = content)
}
