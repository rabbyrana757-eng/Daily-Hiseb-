package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.DefaultCategories
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionType
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetStatus
import com.example.ui.HisabUiState
import com.example.ui.components.AccountSwitcherPill
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.util.CurrencyFormat
import com.example.util.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsHubScreen(
  uiState: HisabUiState,
  selectedSubTab: Int,
  onSelectSubTab: (Int) -> Unit,
  onSwitchAccount: (AccountType) -> Unit,
  onSelectMonthYear: (Int, Int) -> Unit,
  onExportPdf: () -> Unit,
  onExportExcel: () -> Unit,
  onSaveBudget: (existingId: String?, category: String, limitAmount: Double, year: Int, month: Int) -> Unit,
  onDeleteBudget: (String) -> Unit,
  onSaveSavingsGoal: (editingId: String?, name: String, target: Double, saved: Double, dateMillis: Long) -> Unit,
  onAddSavingsContribution: (SavingsGoalRecord, Double) -> Unit,
  onDeleteSavingsGoal: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tabs = listOf("মাসিক রিপোর্ট", "বাজেট", "সঞ্চয়")

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text(
        text = "রিপোর্ট, বাজেট ও সঞ্চয়",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(modifier = Modifier.height(8.dp))
      AccountSwitcherPill(
        activeAccount = uiState.activeAccount,
        onSelectAccount = onSwitchAccount,
      )
    }

    PrimaryTabRow(selectedTabIndex = selectedSubTab.coerceIn(0, 2)) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedSubTab == index,
          onClick = { onSelectSubTab(index) },
          text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.testTag("reports_tab_$index"),
        )
      }
    }

    when (selectedSubTab) {
      0 -> MonthlyReportTab(
        uiState = uiState,
        onSelectMonthYear = onSelectMonthYear,
        onExportPdf = onExportPdf,
        onExportExcel = onExportExcel,
      )
      1 -> BudgetManagementTab(
        uiState = uiState,
        onSaveBudget = onSaveBudget,
        onDeleteBudget = onDeleteBudget,
      )
      else -> SavingsTrackerTab(
        uiState = uiState,
        onSaveSavingsGoal = onSaveSavingsGoal,
        onAddContribution = onAddSavingsContribution,
        onDeleteGoal = onDeleteSavingsGoal,
      )
    }
  }
}

@Composable
private fun MonthlyReportTab(
  uiState: HisabUiState,
  onSelectMonthYear: (Int, Int) -> Unit,
  onExportPdf: () -> Unit,
  onExportExcel: () -> Unit,
) {
  val report = uiState.monthlyReport
  val (prevY, prevM) = DateFormats.previousMonth(report.year, report.month)
  var showMonthYearPicker by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Month & Year Picker Bar
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = {
              val (py, pm) = DateFormats.previousMonth(report.year, report.month)
              onSelectMonthYear(py, pm)
            },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "আগের মাস")
          }
          TextButton(
            onClick = { showMonthYearPicker = true },
            modifier = Modifier.testTag("report_month_picker_button"),
          ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${DateFormats.monthNameBn(report.month)} ${report.year}",
              style = MaterialTheme.typography.titleMedium,
            )
          }
          IconButton(
            onClick = {
              val nextM = if (report.month >= 12) 1 else report.month + 1
              val nextY = if (report.month >= 12) report.year + 1 else report.year
              onSelectMonthYear(nextY, nextM)
            },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "পরের মাস")
          }
        }
      }
    }

    // Export Buttons
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = onExportPdf,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("export_pdf_button"),
        ) {
          Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("PDF এক্সপোর্ট")
        }
        FilledTonalButton(
          onClick = onExportExcel,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("export_excel_button"),
        ) {
          Icon(Icons.Rounded.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Excel এক্সপোর্ট")
        }
      }
    }

    // Main KPIs
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ReportStatCard("মোট আয়", CurrencyFormat.formatBdt(report.totalIncome), IncomeGreen, Modifier.weight(1f))
        ReportStatCard("মোট খরচ", CurrencyFormat.formatBdt(report.totalExpense), ExpenseRed, Modifier.weight(1f))
      }
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ReportStatCard("মোট সঞ্চয়", CurrencyFormat.formatBdt(report.totalSavings), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        ReportStatCard("দৈনিক গড় খরচ", CurrencyFormat.formatBdt(report.averageDailyExpense), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
      }
    }

    // Highest Spending Category & Previous Month Comparison
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("বিশ্লেষণ ও তুলনা", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("সর্বোচ্চ খরচের খাত", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = report.highestCategory?.let { "${it.category} (${CurrencyFormat.formatBdt(it.amount)})" } ?: "তথ্য নেই",
              style = MaterialTheme.typography.titleSmall,
              color = ExpenseRed,
            )
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "আগের মাসের সাথে তুলনা (${DateFormats.monthNameBn(prevM)} $prevY)",
                style = MaterialTheme.typography.labelLarge,
              )
              ComparisonRow("আয় পরিবর্তন", report.incomeDiff, positiveIsGood = true)
              ComparisonRow("খরচ পরিবর্তন", report.expenseDiff, positiveIsGood = false)
              ComparisonRow("সঞ্চয় পরিবর্তন", report.savingsDiff, positiveIsGood = true)
            }
          }
        }
      }
    }

    // Category Breakdown Chart
    if (report.categorySlices.isNotEmpty()) {
      item {
        CategoryPieChart(slices = report.categorySlices)
      }
    }

    // Daily Expense Breakdown List
    item {
      Text(
        text = "দৈনিক আয় ও খরচের বিবরণ",
        style = MaterialTheme.typography.titleMedium,
      )
    }

    if (report.dailyBreakdown.isEmpty()) {
      item {
        EmptyStateCard(
          title = "এই মাসে কোনো লেনদেন নেই",
          subtitle = "অন্য মাস নির্বাচন করুন অথবা নতুন লেনদেন যোগ করুন।",
        )
      }
    } else {
      items(report.dailyBreakdown, key = { it.dateKey }) { dayItem ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(dayItem.dateKey, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              if (dayItem.income > 0) {
                Text(
                  "+${CurrencyFormat.formatCompactBdt(dayItem.income)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = IncomeGreen,
                )
              }
              if (dayItem.expense > 0) {
                Text(
                  "-${CurrencyFormat.formatCompactBdt(dayItem.expense)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = ExpenseRed,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showMonthYearPicker) {
    MonthYearPickerDialog(
      initialYear = report.year,
      initialMonth = report.month,
      onSelect = { y, m ->
        onSelectMonthYear(y, m)
        showMonthYearPicker = false
      },
      onDismiss = { showMonthYearPicker = false },
    )
  }
}

@Composable
private fun BudgetManagementTab(
  uiState: HisabUiState,
  onSaveBudget: (existingId: String?, category: String, limitAmount: Double, year: Int, month: Int) -> Unit,
  onDeleteBudget: (String) -> Unit,
) {
  var showAddBudgetDialog by remember { mutableStateOf(false) }
  var editingBudget by remember { mutableStateOf<BudgetRecord?>(null) }
  var pendingDeleteBudget by remember { mutableStateOf<BudgetStatus?>(null) }

  val now = System.currentTimeMillis()
  val (curYear, curMonth, _) = DateFormats.extractYmd(now)

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "${DateFormats.monthNameBn(curMonth)} $curYear-এর বাজেট",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "৮০% ও ১০০% খরচে স্বয়ংক্রিয় সতর্কতা পাবেন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = {
            editingBudget = null
            showAddBudgetDialog = true
          },
          modifier = Modifier.testTag("add_budget_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("বাজেট সেট")
        }
      }
    }

    if (uiState.budgetStatuses.isEmpty()) {
      item {
        EmptyStateCard(
          title = "কোনো বাজেট সেট করা হয়নি",
          subtitle = "মাসিক মোট বাজেট অথবা খাবার, বাজার ইত্যাদি খাতভিত্তিক বাজেট সেট করুন।",
        )
      }
    } else {
      items(uiState.budgetStatuses, key = { it.budget.id }) { status ->
        BudgetStatusCard(
          status = status,
          onEdit = {
            editingBudget = status.budget
            showAddBudgetDialog = true
          },
          onDelete = { pendingDeleteBudget = status },
        )
      }
    }
  }

  if (showAddBudgetDialog) {
    val customCats =
      uiState.customCategories
        .filter { it.type == TransactionType.EXPENSE.value }
        .map { it.name }
    val allCategories = listOf(BudgetRecord.OVERALL_CATEGORY) + (DefaultCategories.EXPENSE + customCats).distinct()

    BudgetEditorDialog(
      editingBudget = editingBudget,
      categories = allCategories,
      onConfirm = { cat, limit ->
        onSaveBudget(editingBudget?.id, cat, limit, curYear, curMonth)
        showAddBudgetDialog = false
      },
      onDismiss = { showAddBudgetDialog = false },
    )
  }

  pendingDeleteBudget?.let { status ->
    val label =
      if (status.budget.category == BudgetRecord.OVERALL_CATEGORY) "মাসিক মোট বাজেট"
      else status.budget.category
    ConfirmDeleteDialog(
      title = "বাজেট মুছবেন?",
      message = "$label-এর বাজেট (${CurrencyFormat.formatBdt(status.budget.limitAmount)}) মুছে ফেলা হবে।",
      onConfirm = {
        onDeleteBudget(status.budget.id)
        pendingDeleteBudget = null
      },
      onDismiss = { pendingDeleteBudget = null },
    )
  }
}

@Composable
private fun BudgetStatusCard(
  status: BudgetStatus,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val label =
    if (status.budget.category == BudgetRecord.OVERALL_CATEGORY) "মাসিক মোট বাজেট"
    else status.budget.category
  val barColor =
    when (status.alertLevel) {
      BudgetAlertLevel.EXCEEDED_100 -> ExpenseRed
      BudgetAlertLevel.WARNING_80 -> WarningAmber
      BudgetAlertLevel.NORMAL -> MaterialTheme.colorScheme.primary
    }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = "সম্পাদনা", modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = "মুছুন",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      if (status.alertLevel != BudgetAlertLevel.NORMAL) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = barColor.copy(alpha = 0.14f),
          modifier = Modifier.padding(vertical = 4.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = barColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (status.alertLevel == BudgetAlertLevel.EXCEEDED_100) {
                "সতর্কতা: বাজেটের ১০০% অতিক্রম করেছে!"
              } else {
                "সতর্কতা: বাজেটের ${status.percentUsed}% ব্যবহৃত হয়েছে!"
              },
              style = MaterialTheme.typography.labelMedium,
              color = barColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { status.progressFraction },
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp)),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "খরচ: ${CurrencyFormat.formatBdt(status.spent)} (${status.percentUsed}%)",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "সীমা: ${CurrencyFormat.formatBdt(status.budget.limitAmount)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = if (status.remaining >= 0) "অবশিষ্ট: ${CurrencyFormat.formatBdt(status.remaining)}"
        else "অতিরিক্ত খরচ: ${CurrencyFormat.formatBdt(-status.remaining)}",
        style = MaterialTheme.typography.labelMedium,
        color = if (status.remaining >= 0) IncomeGreen else ExpenseRed,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsTrackerTab(
  uiState: HisabUiState,
  onSaveSavingsGoal: (editingId: String?, name: String, target: Double, saved: Double, dateMillis: Long) -> Unit,
  onAddContribution: (SavingsGoalRecord, Double) -> Unit,
  onDeleteGoal: (String) -> Unit,
) {
  val goals = uiState.savingsGoals.filter { it.accountType == uiState.activeAccount.value }
  var showGoalDialog by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<SavingsGoalRecord?>(null) }
  var depositGoal by remember { mutableStateOf<SavingsGoalRecord?>(null) }
  var pendingDeleteGoal by remember { mutableStateOf<SavingsGoalRecord?>(null) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("সঞ্চয় লক্ষ্য ট্র্যাকার", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "আপনার ভবিষ্যৎ লক্ষ্যের জন্য সঞ্চয় ট্র্যাক করুন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = {
            editingGoal = null
            showGoalDialog = true
          },
          modifier = Modifier.testTag("add_savings_goal_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("নতুন লক্ষ্য")
        }
      }
    }

    if (goals.isEmpty()) {
      item {
        EmptyStateCard(
          title = "কোনো সঞ্চয় লক্ষ্য নেই",
          subtitle = "জরুরি তহবিল, ভ্রমণ বা ব্যবসায়িক পুঁজির জন্য একটি সঞ্চয় লক্ষ্য যোগ করুন।",
        )
      }
    } else {
      items(goals, key = { it.id }) { goal ->
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                  Icons.Rounded.Savings,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(goal.name, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "টার্গেট তারিখ: ${goal.targetDateKey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row {
                IconButton(onClick = {
                  editingGoal = goal
                  showGoalDialog = true
                }) {
                  Icon(Icons.Rounded.Edit, contentDescription = "সম্পাদনা", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { pendingDeleteGoal = goal }) {
                  Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "মুছুন",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
              progress = { goal.progressFraction },
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              strokeCap = StrokeCap.Round,
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "জমা: ${CurrencyFormat.formatBdt(goal.savedAmount)} (${goal.progressPercent}%)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "লক্ষ্য: ${CurrencyFormat.formatBdt(goal.targetAmount)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              FilledTonalButton(onClick = { depositGoal = goal }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("জমা যোগ")
              }
            }
          }
        }
      }
    }
  }

  if (showGoalDialog) {
    SavingsGoalEditorDialog(
      editingGoal = editingGoal,
      onConfirm = { name, target, saved, dateMillis ->
        onSaveSavingsGoal(editingGoal?.id, name, target, saved, dateMillis)
        showGoalDialog = false
      },
      onDismiss = { showGoalDialog = false },
    )
  }

  depositGoal?.let { goal ->
    var addText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { depositGoal = null },
      title = { Text("${goal.name} - সঞ্চয় যোগ") },
      text = {
        OutlinedTextField(
          value = addText,
          onValueChange = { s ->
            val filtered = s.filter { it.isDigit() || it == '.' }
            if (filtered.count { it == '.' } <= 1) addText = filtered
          },
          label = { Text("জমার পরিমাণ (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val v = addText.toDoubleOrNull()
            if (v != null && v > 0.0) {
              onAddContribution(goal, v)
              depositGoal = null
            }
          }
        ) {
          Text("যোগ করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { depositGoal = null }) { Text("বাতিল") }
      },
    )
  }

  pendingDeleteGoal?.let { goal ->
    ConfirmDeleteDialog(
      title = "সঞ্চয় লক্ষ্য মুছবেন?",
      message = "\"${goal.name}\" লক্ষ্যটি মুছে ফেলা হবে।",
      onConfirm = {
        onDeleteGoal(goal.id)
        pendingDeleteGoal = null
      },
      onDismiss = { pendingDeleteGoal = null },
    )
  }
}

@Composable
private fun ReportStatCard(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
  }
}

@Composable
private fun ComparisonRow(label: String, diff: Double, positiveIsGood: Boolean) {
  val sign = if (diff >= 0) "+" else "-"
  val good = if (positiveIsGood) diff >= 0 else diff <= 0
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Text(
      text = "$sign${CurrencyFormat.formatBdt(kotlin.math.abs(diff))}",
      style = MaterialTheme.typography.labelLarge,
      color = if (good) IncomeGreen else ExpenseRed,
    )
  }
}

@Composable
private fun MonthYearPickerDialog(
  initialYear: Int,
  initialMonth: Int,
  onSelect: (Int, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var year by remember { mutableStateOf(initialYear) }
  var month by remember { mutableStateOf(initialMonth) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("মাস ও বছর নির্বাচন") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = { year-- }) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "আগের বছর") }
          Text("$year", style = MaterialTheme.typography.titleLarge)
          IconButton(onClick = { year++ }) { Icon(Icons.Rounded.ChevronRight, contentDescription = "পরের বছর") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          for (row in 0 until 4) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              for (col in 0 until 3) {
                val m = row * 3 + col + 1
                val selected = m == month
                OutlinedButton(
                  onClick = { month = m },
                  modifier = Modifier.weight(1f),
                  colors = if (selected) androidx.compose.material3.ButtonDefaults.buttonColors()
                  else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                ) {
                  Text(DateFormats.monthNameBn(m), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSelect(year, month) }) { Text("দেখুন") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    },
  )
}

@Composable
private fun BudgetEditorDialog(
  editingBudget: BudgetRecord?,
  categories: List<String>,
  onConfirm: (category: String, limit: Double) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedCategory by remember { mutableStateOf(editingBudget?.category ?: BudgetRecord.OVERALL_CATEGORY) }
  var limitText by remember {
    mutableStateOf(
      editingBudget?.limitAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
    )
  }
  var showMenu by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (editingBudget == null) "নতুন বাজেট সেট করুন" else "বাজেট সম্পাদনা") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
          OutlinedButton(
            onClick = { showMenu = true },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              if (selectedCategory == BudgetRecord.OVERALL_CATEGORY) "মাসিক মোট বাজেট (সব খাত)"
              else selectedCategory
            )
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = {
                  Text(if (cat == BudgetRecord.OVERALL_CATEGORY) "মাসিক মোট বাজেট (সব খাত)" else cat)
                },
                onClick = {
                  selectedCategory = cat
                  showMenu = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = limitText,
          onValueChange = { s ->
            val filtered = s.filter { it.isDigit() || it == '.' }
            if (filtered.count { it == '.' } <= 1) {
              limitText = filtered
              error = null
            }
          },
          label = { Text("বাজেট সীমা (৳)") },
          isError = error != null,
          supportingText = error?.let { { Text(it) } },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val v = limitText.toDoubleOrNull()
          if (v == null || v <= 0.0) {
            error = "সঠিক বাজেট পরিমাণ লিখুন"
          } else {
            onConfirm(selectedCategory, v)
          }
        }
      ) {
        Text("সংরক্ষণ")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalEditorDialog(
  editingGoal: SavingsGoalRecord?,
  onConfirm: (name: String, target: Double, saved: Double, dateMillis: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(editingGoal?.name.orEmpty()) }
  var targetText by remember {
    mutableStateOf(
      editingGoal?.targetAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
    )
  }
  var savedText by remember {
    mutableStateOf(
      editingGoal?.savedAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "0"
    )
  }
  var targetDateMillis by remember {
    mutableLongStateOf(editingGoal?.targetDateMillis ?: (System.currentTimeMillis() + 86_400_000L * 90))
  }
  var showDatePicker by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (editingGoal == null) "নতুন সঞ্চয় লক্ষ্য" else "লক্ষ্য সম্পাদনা") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 80) { name = it; error = null } },
          label = { Text("লক্ষ্যের নাম (যেমন: জরুরি তহবিল)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = targetText,
          onValueChange = { s ->
            val f = s.filter { it.isDigit() || it == '.' }
            if (f.count { it == '.' } <= 1) { targetText = f; error = null }
          },
          label = { Text("লক্ষ্যমাত্রা (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = savedText,
          onValueChange = { s ->
            val f = s.filter { it.isDigit() || it == '.' }
            if (f.count { it == '.' } <= 1) { savedText = f; error = null }
          },
          label = { Text("বর্তমান সঞ্চিত পরিমাণ (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("সমাপ্তির তারিখ: ${DateFormats.formatDateKey(targetDateMillis)}")
        }
        if (error != null) {
          Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val t = targetText.toDoubleOrNull()
          val s = savedText.toDoubleOrNull() ?: 0.0
          if (name.isBlank()) {
            error = "লক্ষ্যের নাম লিখুন"
          } else if (t == null || t <= 0.0) {
            error = "সঠিক লক্ষ্যমাত্রা লিখুন"
          } else if (s < 0.0) {
            error = "সঞ্চিত পরিমাণ ০ বা বেশি হতে হবে"
          } else {
            onConfirm(name.trim(), t, s, targetDateMillis)
          }
        }
      ) {
        Text("সংরক্ষণ")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    },
  )

  if (showDatePicker) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = targetDateMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { targetDateMillis = it }
          showDatePicker = false
        }) { Text("ঠিক আছে") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
