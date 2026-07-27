package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF00497E),
    onPrimaryContainer = ClinicalBlueLight,
    secondary = SecondaryDark,
    onSecondary = Color(0xFF003735),
    tertiary = TealAccentLight,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = CardSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1E2631),
    onSurfaceVariant = Color(0xFFC3C7CE),
    outline = Color(0xFF43474E),
    outlineVariant = Color(0xFF2E343D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ClinicalBlue,
    onPrimary = Color.White,
    primaryContainer = ClinicalBlueLight,
    onPrimaryContainer = ClinicalBlueDark,
    secondary = TealAccent,
    onSecondary = Color.White,
    tertiary = Color(0xFF006874),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = CardSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SubtleSurfaceLight,
    onSurfaceVariant = MutedSlate,
    outline = BorderColorLight,
    outlineVariant = Color(0xFFEFF3F9)
  )

@Composable
fun PathLabProTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  PathLabProTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
