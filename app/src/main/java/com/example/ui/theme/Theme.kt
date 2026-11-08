package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError
)

data class ExtraColors(
    val accentGold: Color,
    val secondaryText: Color,
    val headerBg: Color = DarkHeader,
    val headerText: Color = DarkHeaderText,
    val headerLine: Color = DarkHeaderLine,
    val barBg: Color = DarkBar,
    val barPill: Color = DarkBarPill
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(accentGold = DarkAccentGold, secondaryText = DarkSecondaryText)
}

val MaterialTheme.extraColors: ExtraColors
    @Composable
    get() = LocalExtraColors.current

@Composable
fun QuranSharifTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extraColors = if (isDark) {
        ExtraColors(accentGold = DarkAccentGold, secondaryText = DarkSecondaryText)
    } else {
        ExtraColors(
            accentGold = LightAccentGold,
            secondaryText = LightSecondaryText,
            headerBg = LightHeader,
            headerText = LightHeaderText,
            headerLine = LightHeaderLine,
            barBg = LightBar,
            barPill = LightBarPill
        )
    }

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuranTypography,
            content = content
        )
    }
}
