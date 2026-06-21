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

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonPurplePrimary,
    secondary = PurpleGlow,
    tertiary = ElevatedSurface,
    background = BackgroundBlack,
    surface = SurfaceGrey,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = TextPrimary,
    outline = DividerColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme only
  dynamicColor: Boolean = false, // Disable dynamic light schemes to preserve intentional branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
