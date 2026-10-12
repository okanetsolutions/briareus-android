package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.okanetsolutions.briareus.tokens.BriareusPalette
import com.okanetsolutions.briareus.tokens.BriareusType

val LocalPalette = staticCompositionLocalOf { BriareusPalette.Dark }
val Mono = FontFamily.Monospace

@Composable
fun BriareusTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) BriareusPalette.Dark else BriareusPalette.Light
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.accentDim, background = p.canvas, onBackground = p.ink,
        surface = p.raise, onSurface = p.ink, surfaceVariant = p.field, onSurfaceVariant = p.muted, outline = p.line,
        outlineVariant = p.line, error = p.danger, surfaceContainer = p.raise, surfaceContainerHigh = p.field,
        surfaceContainerHighest = p.field, surfaceContainerLow = p.sidebar, surfaceContainerLowest = p.sunken,
    )
    val type = Typography(
        bodyLarge = BriareusType.body, bodyMedium = BriareusType.callout, bodySmall = BriareusType.footnote,
        titleLarge = BriareusType.title, titleMedium = BriareusType.title3, titleSmall = BriareusType.headline,
        labelLarge = BriareusType.bodyMedium, labelMedium = BriareusType.captionMedium, labelSmall = BriareusType.caption,
    )
    CompositionLocalProvider(LocalPalette provides p) { MaterialTheme(colorScheme = scheme, typography = type, content = content) }
}
