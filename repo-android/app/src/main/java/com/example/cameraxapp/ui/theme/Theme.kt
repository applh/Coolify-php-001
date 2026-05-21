package com.example.cameraxapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = AccentColor,
    onPrimary = Color.Black,
    surface = Color.Black,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = AccentColor,
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color.Black
)

private val LuminaDarkColorScheme = darkColorScheme(
    primary = LuminaPrimaryDark,
    secondary = LuminaSecondaryDark,
    tertiary = LuminaTertiaryDark,
    background = LuminaBackgroundDark,
    surface = LuminaSurfaceDark,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LuminaLightColorScheme = lightColorScheme(
    primary = LuminaPrimaryLight,
    secondary = LuminaSecondaryLight,
    tertiary = LuminaTertiaryLight,
    background = LuminaBackgroundLight,
    surface = LuminaSurfaceLight,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun CameraXAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: Int = 0, // 0: Standard, 1: Lumina AI Theme
    content: @Composable () -> Unit
) {
    val colorScheme = when (colorTheme) {
        1 -> if (darkTheme) LuminaDarkColorScheme else LuminaLightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
