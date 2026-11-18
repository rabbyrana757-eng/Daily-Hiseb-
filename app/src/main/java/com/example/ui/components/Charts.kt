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
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
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
  NeumorphicCard(
    cornerRadius = 22.dp,
    shadowElevation = 6.dp,
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
      val neo = HisabTheme.neumorphic
      val barBrush = Brush.verticalGradient(neo.barGradient)
      val selectedBrush = Brush.verticalGradient(neo.selectedBarGradient)
      val trackColor = neo.insetSurface

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
        val slotW = w / bars.size.toFloat()
        val barW = (slotW * 0.64f).coerceAtLeast(3.5f)
        val radius = CornerRadius(barW / 2f, barW / 2f)

        bars.forEachIndexed { idx, (_, value) ->
          val left = idx * slotW + (slotW - barW) / 2f
          // Recessed neomorphic capsule track behind every day bar
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, 4f),
            size = Size(barW, h - 4f),
            cornerRadius = radius,
          )
          if (value > 0.0) {
            val ratio = ((value / maxVal).toFloat()).coerceIn(0f, 1f) * progress
            val barH = (ratio * (h - 8f)).coerceAtLeast(barW.coerceAtMost(h - 8f))
            val top = h - barH
            drawRoundRect(
              brush = if (idx == selectedIndex) selectedBrush else barBrush,
              topLeft = Offset(left, top),
              size = Size(barW, barH),
              cornerRadius = radius,
            )
          }
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
      val neo = HisabTheme.neumorphic
      val trackColor = neo.insetSurface
      val incomeBrush = Brush.verticalGradient(listOf(Color(0xFF34D399), IncomeGreen))
      val expenseBrush = Brush.verticalGradient(listOf(Color(0xFFFB7185), ExpenseRed))

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
        val groupW = w / points.size.toFloat()
        val barW = (groupW * 0.28f).coerceAtLeast(10f)
        val gap = (groupW * 0.10f).coerceAtLeast(5f)
        val radius = CornerRadius(barW / 2f, barW / 2f)

        points.forEachIndexed { idx, pt ->
          val centerX = idx * groupW + groupW / 2f
          val incLeft = centerX - barW - gap / 2f
          val expLeft = centerX + gap / 2f

          drawRoundRect(
            color = trackColor,
            topLeft = Offset(incLeft, 4f),
            size = Size(barW, h - 4f),
            cornerRadius = radius,
          )
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(expLeft, 4f),
            size = Size(barW, h - 4f),
            cornerRadius = radius,
          )

          if (pt.income > 0.0) {
            val incH = ((pt.income / maxVal).toFloat() * (h - 8f) * progress).coerceAtLeast(barW.coerceAtMost(h - 8f))
            drawRoundRect(
              brush = incomeBrush,
              topLeft = Offset(incLeft, h - incH),
              size = Size(barW, incH),
              cornerRadius = radius,
            )
          }
          if (pt.expense > 0.0) {
            val expH = ((pt.expense / maxVal).toFloat() * (h - 8f) * progress).coerceAtLeast(barW.coerceAtMost(h - 8f))
            drawRoundRect(
              brush = expenseBrush,
              topLeft = Offset(expLeft, h - expH),
              size = Size(barW, expH),
              cornerRadius = radius,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
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
      val neo = HisabTheme.neumorphic
      val total = slices.sumOf { it.amount }
      val safeSelected = selectedIndex.coerceIn(0, slices.lastIndex)
      val activeSlice = slices[safeSelected]

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(164.dp)
            .padding(6.dp)
            .neumorphicCircle(shadowElevation = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp)
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
            val strokeW = size.minDimension * 0.22f
            val pad = strokeW / 2f + 4f
            val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
            // Recessed ring track behind the donut
            drawArc(
              color = neo.insetSurface,
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = Offset(pad, pad),
              size = arcSize,
              style = Stroke(width = strokeW),
            )
            var startAngle = -90f
            slices.forEachIndexed { idx, slice ->
              val sweep = ((slice.percentage / 100f) * 360f) * sweepScale
              val color = ChartPalette[idx % ChartPalette.size]
              drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(
                  width = if (idx == safeSelected) strokeW * 1.15f else strokeW,
                  cap = StrokeCap.Round,
                ),
              )
              startAngle += sweep
            }
          }
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp),
          ) {
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
              color = MaterialTheme.colorScheme.secondary,
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
                .let { mod ->
                  if (isSel) {
                    mod.neumorphicInset(cornerRadius = 10.dp)
                  } else {
                    mod
                  }
                }
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedIndex = idx }
                .padding(horizontal = 8.dp, vertical = 5.dp),
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
      val neo = HisabTheme.neumorphic
      val primaryLine = MaterialTheme.colorScheme.primary
      val secondaryLine = MaterialTheme.colorScheme.secondary
      val dotHighlight = MaterialTheme.colorScheme.secondary
      val gridColor = MaterialTheme.colorScheme.outlineVariant
      val nodeCenterColor = neo.surface

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
            brush = Brush.verticalGradient(
              listOf(secondaryLine.copy(alpha = 0.22f), primaryLine.copy(alpha = 0.03f), Color.Transparent)
            ),
          )
          drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(primaryLine, secondaryLine)),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
          )
        }

        coords.forEachIndexed { idx, pt ->
          val isSel = idx == selectedIdx
          drawCircle(
            color = if (isSel) dotHighlight else primaryLine,
            radius = if (isSel) 11f else 7f,
            center = pt,
          )
          drawCircle(
            color = nodeCenterColor,
            radius = if (isSel) 5f else 3f,
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
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(100.dp)
      .neumorphicInset(cornerRadius = 14.dp)
      .padding(12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
