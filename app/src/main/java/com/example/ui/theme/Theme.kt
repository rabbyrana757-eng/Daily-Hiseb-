package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldContainerLight,
  onPrimaryContainer = EmeraldOnContainerLight,
  secondary = EmeraldSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE6F7F0),
  onSecondaryContainer = Color(0xFF065F46),
  tertiary = GoldAccent,
  onTertiary = Color.White,
  tertiaryContainer = GoldContainerLight,
  onTertiaryContainer = Color(0xFF78350F),
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightOutline,
  outlineVariant = Color(0xFFE4ECE8),
  error = ExpenseRed,
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  onPrimary = Color(0xFF022C22),
  primaryContainer = EmeraldContainerDark,
  onPrimaryContainer = EmeraldOnContainerDark,
  secondary = EmeraldSecondary,
  onSecondary = Color(0xFF022C22),
  secondaryContainer = Color(0xFF13382C),
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = GoldAccentDark,
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = GoldContainerDark,
  onTertiaryContainer = Color(0xFFFEF3C7),
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkOutline,
  outlineVariant = Color(0xFF1F332B),
  error = ExpenseRedDark,
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFEE2E2),
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DailyHisabTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
