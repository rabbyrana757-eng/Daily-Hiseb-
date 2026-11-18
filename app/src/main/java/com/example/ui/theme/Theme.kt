package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class NeumorphicColors(
  val canvas: Color,
  val surface: Color,
  val insetSurface: Color,
  val highlightShadow: Color,
  val dropShadow: Color,
  val rimHighlight: Color,
  val heroGradient: List<Color>,
  val barGradient: List<Color>,
  val selectedBarGradient: List<Color>,
  val ringGradient: List<Color>,
)

private val LightNeumorphicColors = NeumorphicColors(
  canvas = LightBackground,
  surface = LightSurface,
  insetSurface = LightSurfaceVariant,
  highlightShadow = LightNeoHighlight.copy(alpha = 0.92f),
  dropShadow = LightNeoShadow.copy(alpha = 0.80f),
  rimHighlight = Color.White.copy(alpha = 0.65f),
  heroGradient = listOf(
    Color(0xFF2563EB),
    Color(0xFF4F46E5),
    Color(0xFF7C3AED),
  ),
  barGradient = listOf(
    Color(0xFF38BDF8),
    Color(0xFF2D6EF5),
  ),
  selectedBarGradient = listOf(
    Color(0xFFA855F7),
    Color(0xFF7C3AED),
  ),
  ringGradient = listOf(
    Color(0xFF2D6EF5),
    Color(0xFF7C3AED),
    Color(0xFFEC4899),
  ),
)

private val DarkNeumorphicColors = NeumorphicColors(
  canvas = DarkBackground,
  surface = DarkSurface,
  insetSurface = DarkSurfaceVariant,
  highlightShadow = DarkNeoHighlight.copy(alpha = 0.72f),
  dropShadow = DarkNeoShadow.copy(alpha = 0.88f),
  rimHighlight = Color(0xFF33415C).copy(alpha = 0.55f),
  heroGradient = listOf(
    Color(0xFF1D4ED8),
    Color(0xFF4338CA),
    Color(0xFF6D28D9),
  ),
  barGradient = listOf(
    Color(0xFF38BDF8),
    Color(0xFF60A5FA),
  ),
  selectedBarGradient = listOf(
    Color(0xFFC084FC),
    Color(0xFF8B5CF6),
  ),
  ringGradient = listOf(
    Color(0xFF60A5FA),
    Color(0xFFA78BFA),
    Color(0xFFF472B6),
  ),
)

val LocalNeumorphicColors = staticCompositionLocalOf { LightNeumorphicColors }

object HisabTheme {
  val neumorphic: NeumorphicColors
    @Composable get() = LocalNeumorphicColors.current
}

private val LightColorScheme = lightColorScheme(
  primary = NeoBluePrimary,
  onPrimary = Color.White,
  primaryContainer = NeoBlueContainerLight,
  onPrimaryContainer = NeoBlueOnContainerLight,
  secondary = NeoVioletSecondary,
  onSecondary = Color.White,
  secondaryContainer = NeoVioletContainerLight,
  onSecondaryContainer = NeoVioletOnContainerLight,
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
  outlineVariant = Color(0xFFD5E0EE),
  error = ExpenseRed,
  onError = Color.White,
  errorContainer = Color(0xFFFFE4E6),
  onErrorContainer = Color(0xFF881337),
)

private val DarkColorScheme = darkColorScheme(
  primary = NeoBluePrimaryDark,
  onPrimary = Color(0xFF0A1D45),
  primaryContainer = NeoBlueContainerDark,
  onPrimaryContainer = NeoBlueOnContainerDark,
  secondary = NeoVioletSecondaryDark,
  onSecondary = Color(0xFF211042),
  secondaryContainer = NeoVioletContainerDark,
  onSecondaryContainer = NeoVioletOnContainerDark,
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
  outlineVariant = Color(0xFF253044),
  error = ExpenseRedDark,
  onError = Color(0xFF4C0519),
  errorContainer = Color(0xFF881337),
  onErrorContainer = Color(0xFFFFE4E6),
)

val AppShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun DailyHisabTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val neumorphicColors = if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors

  CompositionLocalProvider(LocalNeumorphicColors provides neumorphicColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content,
    )
  }
}
