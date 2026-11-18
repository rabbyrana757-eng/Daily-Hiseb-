package com.example.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws an extruded (raised) Neomorphic surface with dual soft light/dark shadows
 * and a crisp subtle inner rim highlight.
 */
fun Modifier.neumorphicRaised(
  cornerRadius: Dp = 20.dp,
  shadowElevation: Dp = 6.dp,
  containerColor: Color? = null,
  drawContainer: Boolean = true,
): Modifier = composed {
  val neo = HisabTheme.neumorphic
  val bg = containerColor ?: neo.surface
  val highlight = neo.highlightShadow
  val shadow = neo.dropShadow
  val rim = neo.rimHighlight

  drawBehind {
    val radiusPx = cornerRadius.toPx()
    val offsetPx = shadowElevation.toPx()
    val blurPx = (offsetPx * 1.45f).coerceAtLeast(1f)

    drawIntoCanvas { canvas ->
      val lightPaint = Paint().apply {
        asFrameworkPaint().apply {
          isAntiAlias = true
          color = highlight.toArgb()
          runCatching { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
        }
      }
      canvas.drawRoundRect(
        left = -offsetPx,
        top = -offsetPx,
        right = size.width - offsetPx,
        bottom = size.height - offsetPx,
        radiusX = radiusPx,
        radiusY = radiusPx,
        paint = lightPaint,
      )

      val darkPaint = Paint().apply {
        asFrameworkPaint().apply {
          isAntiAlias = true
          color = shadow.toArgb()
          runCatching { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
        }
      }
      canvas.drawRoundRect(
        left = offsetPx,
        top = offsetPx,
        right = size.width + offsetPx,
        bottom = size.height + offsetPx,
        radiusX = radiusPx,
        radiusY = radiusPx,
        paint = darkPaint,
      )
    }

    if (drawContainer) {
      drawRoundRect(
        color = bg,
        cornerRadius = CornerRadius(radiusPx, radiusPx),
      )
      drawRoundRect(
        color = rim,
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(width = 1.dp.toPx()),
      )
    }
  }
}

/**
 * Draws a raised circular Neomorphic button or badge.
 */
fun Modifier.neumorphicCircle(
  shadowElevation: Dp = 5.dp,
  containerColor: Color? = null,
): Modifier = composed {
  val neo = HisabTheme.neumorphic
  val bg = containerColor ?: neo.surface
  val highlight = neo.highlightShadow
  val shadow = neo.dropShadow
  val rim = neo.rimHighlight

  drawBehind {
    val offsetPx = shadowElevation.toPx()
    val blurPx = (offsetPx * 1.4f).coerceAtLeast(1f)
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    drawIntoCanvas { canvas ->
      val lightPaint = Paint().apply {
        asFrameworkPaint().apply {
          isAntiAlias = true
          color = highlight.toArgb()
          runCatching { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
        }
      }
      canvas.drawCircle(
        center = Offset(center.x - offsetPx, center.y - offsetPx),
        radius = radius,
        paint = lightPaint,
      )

      val darkPaint = Paint().apply {
        asFrameworkPaint().apply {
          isAntiAlias = true
          color = shadow.toArgb()
          runCatching { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
        }
      }
      canvas.drawCircle(
        center = Offset(center.x + offsetPx, center.y + offsetPx),
        radius = radius,
        paint = darkPaint,
      )
    }

    drawCircle(color = bg, radius = radius, center = center)
    drawCircle(
      color = rim,
      radius = (radius - 0.5.dp.toPx()).coerceAtLeast(1f),
      center = center,
      style = Stroke(width = 1.dp.toPx()),
    )
  }
}

/**
 * Draws a recessed (pressed/inset) Neomorphic well for tracks, pill containers, and summary bars.
 */
fun Modifier.neumorphicInset(
  cornerRadius: Dp = 16.dp,
  containerColor: Color? = null,
): Modifier = composed {
  val neo = HisabTheme.neumorphic
  val bg = containerColor ?: neo.insetSurface
  val shadow = neo.dropShadow
  val highlight = neo.highlightShadow

  drawBehind {
    val r = cornerRadius.toPx()
    drawRoundRect(
      color = bg,
      cornerRadius = CornerRadius(r, r),
    )
    // Top-left inner depth rim
    drawRoundRect(
      color = shadow.copy(alpha = 0.45f),
      topLeft = Offset(0.8.dp.toPx(), 0.8.dp.toPx()),
      size = size.copy(
        width = (size.width - 1.6.dp.toPx()).coerceAtLeast(1f),
        height = (size.height - 1.6.dp.toPx()).coerceAtLeast(1f),
      ),
      cornerRadius = CornerRadius(r, r),
      style = Stroke(width = 1.6.dp.toPx()),
    )
    // Bottom-right bright highlight bevel
    drawRoundRect(
      color = highlight.copy(alpha = 0.55f),
      cornerRadius = CornerRadius(r, r),
      style = Stroke(width = 1.dp.toPx()),
    )
  }
}
