package com.jhovanny.musicplayback.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * A custom dark color scheme designed specifically for this application.
 * It is configured to ensure high visibility of UI elements against a video background.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,

    // The main background is set to transparent to allow the video background to show through.
    background = Color.Transparent,

    // Surfaces, like cards or bottom sheets, will use a semi-transparent white color.
    surface = surfaceWhite,

    // Key text colors are forced to white to ensure readability over the dark background.
    onBackground = TextWhite,
    onSurface = TextWhite,
    onPrimary = Color.White // Text on top of primary-colored elements (e.g., buttons).
)

/**
 * The main theme for the Music Playback application.
 *
 * This theme enforces a custom dark mode and sets up system UI components for an
 * immersive, edge-to-edge experience. It ignores the device's system theme settings
 * to maintain a consistent look and feel.
 *
 * @param content The composable content to be displayed within this theme.
 */
@Composable
fun MusicPlaybackTheme(
    // The default `darkTheme: Boolean = isSystemInDarkTheme()` parameter is intentionally omitted
    // to enforce the custom dark theme regardless of system settings.
    content: @Composable () -> Unit
) {
    // Always apply the custom dark color scheme.
    val colorScheme = DarkColorScheme

    // This SideEffect configures the system UI (status bar) for a consistent appearance.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set the status bar icons (time, battery) to be light (e.g., white).
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Apply the MaterialTheme with the custom color scheme to the content.
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}