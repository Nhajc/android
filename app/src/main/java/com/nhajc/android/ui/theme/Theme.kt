package com.nhajc.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NhajcDarkColorScheme = darkColorScheme(
    primary = Color(0xFF1ED760),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF117A37),
    secondary = Color(0xFF1ED760),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF1ED760),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF121212),
    surfaceBright = Color(0xFF383838),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    outline = Color(0xFF727272),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFFF8A9A),
    onError = Color(0xFF4A0014),
    errorContainer = Color(0xFF4B1420),
    onErrorContainer = Color(0xFFFFD9DF),
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF121212)
)

private val NhajcLightColorScheme = lightColorScheme(
    primary = Color(0xFF087F36),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F7EA),
    onPrimaryContainer = Color(0xFF0B5127),
    inversePrimary = Color(0xFF1ED760),
    secondary = Color(0xFF087F36),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF121212),
    tertiary = Color(0xFF087F36),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF121212),
    background = Color.White,
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5E5E5E),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE0E0E0),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    outline = Color(0xFF878787),
    outlineVariant = Color(0xFFDDDDDD),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color.White
)

@Composable
fun NhajcTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        NhajcDarkColorScheme
    } else {
        NhajcLightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
