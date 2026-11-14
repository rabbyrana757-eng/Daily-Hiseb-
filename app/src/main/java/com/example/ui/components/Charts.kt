package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CategorySlice
import com.example.domain.DailyTrendPoint
import com.example.domain.MonthlyBarPoint
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormat
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun ChartCardContainer(
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      content()
    }
  }
}

@Composable
fun DailyExpenseBarChart(
  bars: List<Pair<Int, Double>>,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember { mutableIntStateOf(-1) }
  var animateTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(bars) { animateTrigger = true }
  val progress by animateFloatAsState(
    targetValue = if (animateTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 550),
    label = "daily_bars_anim",
  )

  ChartCardContainer(
    title = "দৈনিক খরচের চার্ট (এই মাস)",
    subtitle = if (selectedIndex in bars.indices) {
      val (d, v) = bars[selectedIndex]
      "দিন $d: ${CurrencyFormat.formatBdt(v)}"
    } else {
      "যেকোনো বারে ট্যাপ করে সেই দিনের খরচ দেখুন"
    },
    modifier = modifier,
  ) {
    val maxVal = (bars.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val hasData = bars.any { it.second > 0.0 }
    if (!hasData) {
      ChartEmptyHint("এই মাসে এখনো কোনো খরচের তথ্য নেই")
    } else {
      val barColor = MaterialTheme.colorScheme.primary
      val selectedColor = MaterialTheme.colorScheme.tertiary
      val gridColor = MaterialTheme.colorScheme.outlineVariant

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .pointerInput(bars) {
            detectTapGestures { offset ->
              if (bars.isNotEmpty()) {
                val slot = size.width / bars.size.toFloat()
                val idx = (offset.x / slot).toInt().coerceIn(0, bars.lastIndex)
                selectedIndex = idx
              }
            }
          }
      ) {
        val h = size.height
        val w = size.width
        drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1.5f)

        val slotW = w / bars.size.toFloat()
        val barW = (slotW * 0.62f).coerceAtLeast(3f)
        bars.forEachIndexed { idx, (_, value) ->
          val ratio = ((value / maxVal).toFloat()).coerceIn(0f, 1f) * progress
          val barH = (ratio * (h - 14f)).coerceAtLeast(if (value > 0) 4f else 0f)
          val left = idx * slotW + (slotW - barW) / 2f
          val top = h - barH
          drawRoundRect(
            color = if (idx == selectedIndex) selectedColor else barColor,
            topLeft = Offset(left, top),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(6f, 6f),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("১", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("১০", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("২০", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${bars.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun MonthlyIncomeExpenseChart(
  points: List<MonthlyBarPoint>,
  modifier: Modifier = Modifier,
) {
  var selectedIdx by remember { mutableIntStateOf(points.lastIndex) }
  var animateTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(points) { animateTrigger = true }
  val progress by animateFloatAsState(
    targetValue = if (animateTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 600),
    label = "monthly_compare_anim",
  )

  ChartCardContainer(
    title = "মাসিক আয় বনাম খরচ",
    subtitle = if (selectedIdx in points.indices) {
      val pt = points[selectedIdx]
      "${pt.label}: আয় ${CurrencyFormat.formatCompactBdt(pt.income)} | খরচ ${CurrencyFormat.formatCompactBdt(pt.expense)}"
    } else "গত ৬ মাসের তুলনা",
    modifier = modifier,
  ) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    if (points.all { it.income == 0.0 && it.expense == 0.0 }) {
      ChartEmptyHint("তুলনা দেখানোর জন্য লেনদেন যোগ করুন")
    } else {
      val gridColor = MaterialTheme.colorScheme.outlineVariant
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .pointerInput(points) {
            detectTapGestures { offset ->
              if (points.isNotEmpty()) {
                val slot = size.width / points.size.toFloat()
                selectedIdx = (offset.x / slot).toInt().coerceIn(0, points.lastIndex)
              }
            }
          }
      ) {
        val h = size.height
        val w = size.width
        drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1.5f)

        val groupW = w / points.size.toFloat()
        val barW = (groupW * 0.28f).coerceAtLeast(8f)
        val gap = (groupW * 0.08f).coerceAtLeast(4f)
        points.forEachIndexed { idx, pt ->
          val centerX = idx * groupW + groupW / 2f
          val incH = ((pt.income / maxVal).toFloat() * (h - 14f) * progress).coerceAtLeast(if (pt.income > 0) 4f else 0f)
          val expH = ((pt.expense / maxVal).toFloat() * (h - 14f) * progress).coerceAtLeast(if (pt.expense > 0) 4f else 0f)

          drawRoundRect(
            color = IncomeGreen,
            topLeft = Offset(centerX - barW - gap / 2f, h - incH),
            size = Size(barW, incH),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawRoundRect(
            color = ExpenseRed,
            topLeft = Offset(centerX + gap / 2f, h - expH),
            size = Size(barW, expH),
            cornerRadius = CornerRadius(6f, 6f),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        points.forEachIndexed { idx, pt ->
          Text(
            text = pt.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
            color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LegendDot(color = IncomeGreen, label = "আয়")
        LegendDot(color = ExpenseRed, label = "খরচ")
      }
    }
  }
}

@Composable
fun CategoryPieChart(
  slices: List<CategorySlice>,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember { mutableIntStateOf(0) }
  var animateTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(slices) { animateTrigger = true }
  val sweepScale by animateFloatAsState(
    targetValue = if (animateTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 650),
    label = "pie_anim",
  )

  ChartCardContainer(
    title = "ক্যাটাগরি অনুযায়ী খরচ",
    subtitle = "এই মাসের খাতভিত্তিক খরচের অনুপাত",
    modifier = modifier,
  ) {
    if (slices.isEmpty()) {
      ChartEmptyHint("এই মাসে কোনো খরচ রেকর্ড হয়নি")
    } else {
      val total = slices.sumOf { it.amount }
      val safeSelected = selectedIndex.coerceIn(0, slices.lastIndex)
      val activeSlice = slices[safeSelected]

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(160.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(slices) {
                detectTapGestures { offset ->
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val dx = offset.x - cx
                  val dy = offset.y - cy
                  val dist = sqrt(dx * dx + dy * dy)
                  val radius = min(size.width, size.height) / 2f
                  if (dist in (radius * 0.35f)..radius) {
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (angle < 0f) angle += 360f
                    var acc = 0f
                    slices.forEachIndexed { idx, slice ->
                      val sweep = (slice.percentage / 100f) * 360f
                      if (angle in acc..(acc + sweep)) {
                        selectedIndex = idx
                        return@detectTapGestures
                      }
                      acc += sweep
                    }
                  }
                }
              }
          ) {
            val strokeW = size.minDimension * 0.24f
            val pad = strokeW / 2f + 4f
            val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
            var startAngle = -90f
            slices.forEachIndexed { idx, slice ->
              val sweep = ((slice.percentage / 100f) * 360f) * sweepScale
              val color = ChartPalette[idx % ChartPalette.size]
              drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweep - 1.5f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = if (idx == safeSelected) strokeW * 1.15f else strokeW, cap = StrokeCap.Butt),
              )
              startAngle += sweep
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = activeSlice.category,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${activeSlice.percentage.toInt()}%",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = CurrencyFormat.formatCompactBdt(total),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          slices.take(6).forEachIndexed { idx, slice ->
            val color = ChartPalette[idx % ChartPalette.size]
            val isSel = idx == safeSelected
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                  else Color.Transparent
                )
                .clickable { selectedIndex = idx }
                .padding(horizontal = 6.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(color)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = slice.category,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = CurrencyFormat.formatCompactBdt(slice.amount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SevenDayTrendLineChart(
  points: List<DailyTrendPoint>,
  modifier: Modifier = Modifier,
) {
  var selectedIdx by remember { mutableIntStateOf(points.lastIndex) }
  var animateTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(points) { animateTrigger = true }
  val progress by animateFloatAsState(
    targetValue = if (animateTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 600),
    label = "trend_anim",
  )

  ChartCardContainer(
    title = "গত ৭ দিনের খরচের ধারা",
    subtitle = if (selectedIdx in points.indices) {
      val pt = points[selectedIdx]
      "${pt.dateKey}: ${CurrencyFormat.formatBdt(pt.amount)}"
    } else "দৈনিক ব্যয়ের প্রবণতা",
    modifier = modifier,
  ) {
    val maxVal = (points.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0)
    if (points.all { it.amount == 0.0 }) {
      ChartEmptyHint("গত ৭ দিনে কোনো খরচ হয়নি")
    } else {
      val lineColor = MaterialTheme.colorScheme.primary
      val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
      val dotHighlight = MaterialTheme.colorScheme.tertiary
      val gridColor = MaterialTheme.colorScheme.outlineVariant

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(155.dp)
          .pointerInput(points) {
            detectTapGestures { offset ->
              if (points.isNotEmpty()) {
                val step = size.width / (points.size - 1).coerceAtLeast(1)
                selectedIdx = ((offset.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
              }
            }
          }
      ) {
        val h = size.height
        val w = size.width
        val topPad = 14f
        val bottomPad = 14f
        val usableH = (h - topPad - bottomPad).coerceAtLeast(10f)
        drawLine(gridColor, Offset(0f, h - bottomPad), Offset(w, h - bottomPad), strokeWidth = 1.5f)
        drawLine(gridColor, Offset(0f, topPad + usableH / 2f), Offset(w, topPad + usableH / 2f), strokeWidth = 1f)

        val coords = points.mapIndexed { idx, pt ->
          val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
          val norm = (pt.amount / maxVal).toFloat() * progress
          val y = topPad + usableH * (1f - norm)
          Offset(x, y)
        }

        if (coords.size >= 2) {
          val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.lastIndex) {
              val p0 = coords[i]
              val p1 = coords[i + 1]
              val midX = (p0.x + p1.x) / 2f
              cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
          }
          val areaPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, h - bottomPad)
            lineTo(coords.first().x, h - bottomPad)
            close()
          }
          drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)),
          )
          drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 5f, cap = StrokeCap.Round),
          )
        }

        coords.forEachIndexed { idx, pt ->
          val isSel = idx == selectedIdx
          drawCircle(
            color = if (isSel) dotHighlight else lineColor,
            radius = if (isSel) 10f else 6f,
            center = pt,
          )
          drawCircle(
            color = Color.White,
            radius = if (isSel) 4.5f else 2.5f,
            center = pt,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        points.forEachIndexed { idx, pt ->
          Text(
            text = pt.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
            color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ChartEmptyHint(text: String) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = Modifier
      .fillMaxWidth()
      .height(100.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
