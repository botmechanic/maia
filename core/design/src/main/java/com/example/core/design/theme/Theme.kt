package com.example.core.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CompositionLocal for accessing Maia's semantic design tokens.
 * Use MaiaTheme.colors in composables to access earth-tone semantic roles.
 */
val LocalMaiaColors = staticCompositionLocalOf { LightMaiaColors }

/**
 * Accessor object for Maia theme values.
 * Usage: MaiaTheme.colors.canvas, MaiaTheme.colors.maiaPresence, etc.
 */
object MaiaTheme {
    val colors: MaiaColors
        @Composable
        get() = LocalMaiaColors.current
}

/**
 * Maps MaiaColors semantic roles to Material3 ColorScheme.
 * This ensures Material3 stock components inherit the earth-tone system.
 */
private fun createMaterialColorScheme(
    maiaColors: MaiaColors,
    isDark: Boolean
) = if (isDark) {
    darkColorScheme(
        primary = maiaColors.maiaPresence,
        onPrimary = Color.White,
        primaryContainer = maiaColors.userBubble,
        onPrimaryContainer = maiaColors.ink,
        secondary = maiaColors.inkMuted,
        onSecondary = Color.White,
        tertiary = maiaColors.maiaGlow,
        onTertiary = maiaColors.ink,
        background = maiaColors.canvas,
        onBackground = maiaColors.ink,
        surface = maiaColors.surface,
        onSurface = maiaColors.ink,
        surfaceVariant = maiaColors.userBubble,
        onSurfaceVariant = maiaColors.inkMuted,
        outline = maiaColors.hairline,
        outlineVariant = maiaColors.hairline.copy(alpha = 0.5f),
        error = maiaColors.danger,
        onError = Color.White,
    )
} else {
    lightColorScheme(
        primary = maiaColors.maiaPresence,
        onPrimary = Color.White,
        primaryContainer = maiaColors.userBubble,
        onPrimaryContainer = maiaColors.ink,
        secondary = maiaColors.inkMuted,
        onSecondary = Color.White,
        tertiary = maiaColors.maiaGlow,
        onTertiary = maiaColors.ink,
        background = maiaColors.canvas,
        onBackground = maiaColors.ink,
        surface = maiaColors.surface,
        onSurface = maiaColors.ink,
        surfaceVariant = maiaColors.userBubble,
        onSurfaceVariant = maiaColors.inkMuted,
        outline = maiaColors.hairline,
        outlineVariant = maiaColors.hairline.copy(alpha = 0.5f),
        error = maiaColors.danger,
        onError = Color.White,
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val maiaColors = if (darkTheme) DarkMaiaColors else LightMaiaColors
    val colorScheme = createMaterialColorScheme(maiaColors, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = maiaColors.canvas.toArgb()
            window.navigationBarColor = maiaColors.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMaiaColors provides maiaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
