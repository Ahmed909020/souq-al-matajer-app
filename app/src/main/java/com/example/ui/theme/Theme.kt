package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val SouqLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = OrangeAccent,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = BackgroundSoft,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed,
)

val SouqDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = OrangeAccentDark,
    onSecondary = OnOrangeDark,
    secondaryContainer = OrangeContainerDark,
    onSecondaryContainer = OnOrangeContainerDark,
    tertiary = EmeraldPrimary,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark,
    error = DangerRedDark,
    onError = OnDangerDark,
    errorContainer = DangerContainerDark,
    onErrorContainer = OnDangerContainerDark,
)

private val SouqShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun SouqAlMatagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SouqDarkColorScheme else SouqLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SouqTypography,
            shapes = SouqShapes,
            content = content,
        )
    }
}
