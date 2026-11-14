package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.TransactionRecord
import com.example.domain.BudgetAlertLevel
import com.example.ui.HisabUiState
import com.example.ui.components.AccountSwitcherPill
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.DailyExpenseBarChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthlyIncomeExpenseChart
import com.example.ui.components.SevenDayTrendLineChart
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.util.CurrencyFormat

@Composable
fun DashboardScreen(
  uiState: HisabUiState,
  isDarkTheme: Boolean,
  userName: String?,
  onToggleTheme: () -> Unit,
  onSwitchAccount: (AccountType) -> Unit,
  onAddTransaction: () -> Unit,
  onEditTransaction: (TransactionRecord) -> Unit,
  onOpenTransactions: () -> Unit,
  onOpenReportsTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val summary = uiState.dashboard
  val recentTxs =
    uiState.transactions.filter { it.accountType == uiState.activeAccount.value }.take(5)
  val accountGoals =
    uiState.savingsGoals.filter { it.accountType == uiState.activeAccount.value }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 700.dp

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Top Greeting + Theme Toggle
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "My Daily হিসাব",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = if (!userName.isNullOrBlank()) "স্বাগতম, $userName" else "আপনার দৈনিক আর্থিক খাতা",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
              .size(48.dp)
              .testTag("dashboard_theme_toggle"),
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
              contentDescription = if (isDarkTheme) "লাইট মোড" else "ডার্ক মোড",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // Personal / Business Account Switcher
      item {
        AccountSwitcherPill(
          activeAccount = uiState.activeAccount,
          onSelectAccount = onSwitchAccount,
        )
      }

      // Budget Alert Banner (80% / 100%)
      if (uiState.triggeredBudgetAlerts.isNotEmpty()) {
        item {
          AnimatedVisibility(visible = true) {
            val topAlert = uiState.triggeredBudgetAlerts.first()
            val isExceeded = topAlert.alertLevel == BudgetAlertLevel.EXCEEDED_100
            val containerColor =
              if (isExceeded) MaterialTheme.colorScheme.errorContainer
              else Color(0xFFFEF3C7)
            val contentColor =
              if (isExceeded) MaterialTheme.colorScheme.onErrorContainer
              else Color(0xFF92400E)
            val catLabel =
              if (topAlert.budget.category == BudgetRecord.OVERALL_CATEGORY) "মাসিক মোট বাজেট"
              else topAlert.budget.category

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = containerColor,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenReportsTab(1) }
                .testTag("budget_warning_banner"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.WarningAmber,
                  contentDescription = "বাজেট সতর্কতা",
                  tint = if (isExceeded) ExpenseRed else WarningAmber,
                  modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isExceeded) "বাজেট সীমা অতিক্রম করেছে!" else "বাজেটের ${topAlert.percentUsed}% খরচ হয়েছে",
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                  )
                  Text(
                    text = "$catLabel: ${CurrencyFormat.formatBdt(topAlert.spent)} / ${CurrencyFormat.formatBdt(topAlert.budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                  contentDescription = "বাজেট দেখুন",
                  tint = contentColor,
                )
              }
            }
          }
        }
      }

      // Hero Total Available Balance + Today's Balance Card
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF064E3B),
                  Color(0xFF0B6E4F),
                  Color(0xFF10B981),
                )
              )
            )
            .padding(20.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.AccountBalanceWallet,
                  contentDescription = null,
                  tint = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "মোট ব্যালেন্স (${uiState.activeAccount.labelBn})",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color.White.copy(alpha = 0.9f),
                )
              }
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                  .clip(RoundedCornerShape(20.dp))
                  .clickable(onClick = onAddTransaction)
                  .testTag("hero_quick_add_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "নতুন এন্ট্রি",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = CurrencyFormat.formatBdt(summary.totalAvailableBalance, showDecimalsIfWhole = false),
              style = MaterialTheme.typography.displaySmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              HeroMiniMetric(
                label = "আজকের আয়",
                value = CurrencyFormat.formatBdt(summary.todayIncome),
                modifier = Modifier.weight(1f),
              )
              HeroMiniMetric(
                label = "আজকের খরচ",
                value = CurrencyFormat.formatBdt(summary.todayExpense),
                modifier = Modifier.weight(1f),
              )
              HeroMiniMetric(
                label = "আজকের ব্যালেন্স",
                value = CurrencyFormat.formatBdt(summary.todayBalance),
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Monthly KPI Cards (This Month's Income, Expense, Monthly Savings)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiSummaryCard(
            title = "এই মাসের আয়",
            value = CurrencyFormat.formatBdt(summary.monthIncome),
            accentColor = IncomeGreen,
            icon = Icons.Rounded.ArrowDownward,
            modifier = Modifier.weight(1f),
          )
          KpiSummaryCard(
            title = "এই মাসের খরচ",
            value = CurrencyFormat.formatBdt(summary.monthExpense),
            accentColor = ExpenseRed,
            icon = Icons.Rounded.ArrowUpward,
            modifier = Modifier.weight(1f),
          )
          KpiSummaryCard(
            title = "মাসিক সঞ্চয়",
            value = CurrencyFormat.formatBdt(summary.monthSavings),
            accentColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Rounded.Savings,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Interactive Charts Section
      if (isWide) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            DailyExpenseBarChart(
              bars = summary.dailyExpenseBars,
              modifier = Modifier.weight(1f),
            )
            MonthlyIncomeExpenseChart(
              points = summary.monthlyComparisonBars,
              modifier = Modifier.weight(1f),
            )
          }
        }
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            CategoryPieChart(
              slices = summary.categorySlices,
              modifier = Modifier.weight(1f),
            )
            SevenDayTrendLineChart(
              points = summary.last7DaysTrend,
              modifier = Modifier.weight(1f),
            )
          }
        }
      } else {
        item {
          SevenDayTrendLineChart(points = summary.last7DaysTrend)
        }
        item {
          CategoryPieChart(slices = summary.categorySlices)
        }
        item {
          DailyExpenseBarChart(bars = summary.dailyExpenseBars)
        }
        item {
          MonthlyIncomeExpenseChart(points = summary.monthlyComparisonBars)
        }
      }

      // Savings Goals Preview Strip
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "সঞ্চয় লক্ষ্য",
                style = MaterialTheme.typography.titleMedium,
              )
              TextButton(onClick = { onOpenReportsTab(2) }) {
                Text("সব দেখুন")
              }
            }
            if (accountGoals.isEmpty()) {
              Text(
                text = "কোনো সঞ্চয় লক্ষ্য সেট করা হয়নি। নতুন লক্ষ্য যোগ করতে ট্যাপ করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              FilledTonalButton(onClick = { onOpenReportsTab(2) }) {
                Text("লক্ষ্য যোগ করুন")
              }
            } else {
              accountGoals.take(2).forEach { goal ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(goal.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${goal.progressPercent}% (${CurrencyFormat.formatCompactBdt(goal.savedAmount)} / ${CurrencyFormat.formatCompactBdt(goal.targetAmount)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                  progress = { goal.progressFraction },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  strokeCap = StrokeCap.Round,
                )
              }
            }
          }
        }
      }

      // Recent Transactions
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "সাম্প্রতিক লেনদেন",
            style = MaterialTheme.typography.titleMedium,
          )
          TextButton(onClick = onOpenTransactions) {
            Text("সব দেখুন")
          }
        }
      }

      if (recentTxs.isEmpty()) {
        item {
          EmptyStateCard(
            title = "এখনো কোনো লেনদেন নেই",
            subtitle = "নিচের + বোতামে ট্যাপ করে আপনার প্রথম আয় বা খরচ যোগ করুন।",
          )
        }
      } else {
        items(recentTxs.size, key = { recentTxs[it].id }) { idx ->
          val tx = recentTxs[idx]
          CompactTransactionRow(
            tx = tx,
            onClick = { onEditTransaction(tx) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun HeroMiniMetric(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.85f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun KpiSummaryCard(
  title: String,
  value: String,
  accentColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.14f),
        modifier = Modifier.size(32.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CompactTransactionRow(
  tx: TransactionRecord,
  onClick: () -> Unit,
) {
  val amountColor = if (tx.isIncome) IncomeGreen else ExpenseRed
  val sign = if (tx.isIncome) "+" else "-"
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = amountColor.copy(alpha = 0.12f),
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (tx.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
            contentDescription = null,
            tint = amountColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.category,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = buildString {
            append(tx.dateKey)
            append(" • ")
            append(tx.paymentMethod)
            if (!tx.note.isNullOrBlank()) {
              append(" • ")
              append(tx.note)
            }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "$sign${CurrencyFormat.formatBdt(tx.amount)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = amountColor,
      )
    }
  }
}
