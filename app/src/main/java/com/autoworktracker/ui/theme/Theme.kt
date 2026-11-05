package com.autoworktracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.autoworktracker.domain.model.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = Ink,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = Color(0xFFB8FFE6),
    secondary = Blue,
    onSecondary = Color(0xFF041226),
    secondaryContainer = BlueDeep,
    onSecondaryContainer = Color(0xFFD6E2FF),
    tertiary = Violet,
    onTertiary = VioletInk,
    tertiaryContainer = Color(0xFF3A2760),
    onTertiaryContainer = Color(0xFFE8D9FF),
    background = Night,
    onBackground = TextPrimary,
    surface = NightRaised,
    onSurface = TextPrimary,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBlue,
    outlineVariant = Color(0xFF24314E),
    error = Danger,
    onError = Color.White,
    errorContainer = DangerInk,
    onErrorContainer = Color(0xFFFFD6DF),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF067A56),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8FFE9),
    onPrimaryContainer = Color(0xFF043528),
    secondary = Color(0xFF2E5BFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF0C1B44),
    tertiary = Color(0xFF6E4BB8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8D9FF),
    onTertiaryContainer = VioletInk,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightMuted,
    outline = Color(0xFFC5D0E2),
    error = Color(0xFFBA1A3A),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9E1),
    onErrorContainer = Color(0xFF400014),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun AutoWorkTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val view = LocalView.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = view.context
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
