package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkMossPrimary,
    secondary = DarkMossSecondary,
    tertiary = DarkMossGold,
    background = DarkMossBg,
    surface = DarkMossSurface,
    onPrimary = Color(0xFF00381E),
    onSecondary = Color(0xFF00381E),
    onTertiary = Color(0xFF3F2E00),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = GoldAccent,
    background = SoftMintBg,
    surface = SurfaceCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color so we enforce our gorgeous branded Madrasah palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
