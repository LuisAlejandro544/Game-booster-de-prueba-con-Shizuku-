package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ClassicNavyPrimaryDark,
    onPrimary = ClassicNavyOnPrimaryDark,
    primaryContainer = ClassicNavyPrimaryContainerDark,
    onPrimaryContainer = ClassicNavyOnPrimaryContainerDark,
    secondary = ClassicSlateSecondaryDark,
    onSecondary = ClassicSlateOnSecondaryDark,
    secondaryContainer = ClassicSlateSecondaryContainerDark,
    onSecondaryContainer = ClassicSlateOnSecondaryContainerDark,
    tertiary = ClassicSteelTertiaryDark,
    onTertiary = ClassicSteelOnTertiaryDark,
    background = ClassicBackgroundDark,
    surface = ClassicSurfaceDark,
    onBackground = ClassicOnSurfaceDark,
    onSurface = ClassicOnSurfaceDark,
    outline = ClassicOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ClassicNavyPrimary,
    onPrimary = ClassicNavyOnPrimary,
    primaryContainer = ClassicNavyPrimaryContainer,
    onPrimaryContainer = ClassicNavyOnPrimaryContainer,
    secondary = ClassicSlateSecondary,
    onSecondary = ClassicSlateOnSecondary,
    secondaryContainer = ClassicSlateSecondaryContainer,
    onSecondaryContainer = ClassicSlateOnSecondaryContainer,
    tertiary = ClassicSteelTertiary,
    onTertiary = ClassicSteelOnTertiary,
    background = ClassicBackgroundLight,
    surface = ClassicSurfaceLight,
    onBackground = ClassicOnSurfaceLight,
    onSurface = ClassicOnSurfaceLight,
    outline = ClassicOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
