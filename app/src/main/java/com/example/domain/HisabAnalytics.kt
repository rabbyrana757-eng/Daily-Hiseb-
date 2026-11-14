package com.example.domain

import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.util.DateFormats
import java.util.Calendar

data class DashboardSummary(
  val todayIncome: Double = 0.0,
  val todayExpense: Double = 0.0,
  val todayBalance: Double = 0.0,
  val monthIncome: Double = 0.0,
  val monthExpense: Double = 0.0,
  val monthSavings: Double = 0.0,
  val totalAvailableBalance: Double = 0.0,
  val dailyExpenseBars: List<Pair<Int, Double>> = emptyList(),
  val monthlyComparisonBars: List<MonthlyBarPoint> = emptyList(),
  val categorySlices: List<CategorySlice> = emptyList(),
  val last7DaysTrend: List<DailyTrendPoint> = emptyList(),
)

data class MonthlyBarPoint(
  val year: Int,
  val month: Int,
  val label: String,
  val income: Double,
  val expense: Double,
)

data class CategorySlice(
  val category: String,
  val amount: Double,
  val percentage: Float,
)

data class DailyTrendPoint(
  val dateKey: String,
  val label: String,
  val amount: Double,
)

data class DailyBreakdownItem(
  val day: Int,
  val dateKey: String,
  val income: Double,
  val expense: Double,
)

data class MonthlyReportData(
  val year: Int,
  val month: Int,
  val totalIncome: Double,
  val totalExpense: Double,
  val totalSavings: Double,
  val highestCategory: CategorySlice?,
  val averageDailyExpense: Double,
  val dailyBreakdown: List<DailyBreakdownItem>,
  val categorySlices: List<CategorySlice>,
  val prevIncome: Double,
  val prevExpense: Double,
  val prevSavings: Double,
  val incomeDiff: Double,
  val expenseDiff: Double,
  val savingsDiff: Double,
)

enum class BudgetAlertLevel {
  NORMAL,
  WARNING_80,
  EXCEEDED_100,
}

data class BudgetStatus(
  val budget: BudgetRecord,
  val spent: Double,
  val remaining: Double,
  val percentUsed: Int,
  val progressFraction: Float,
  val alertLevel: BudgetAlertLevel,
)

object HisabAnalytics {

  fun computeDashboard(
    allTransactions: List<TransactionRecord>,
    accountType: AccountType,
    nowMillis: Long = System.currentTimeMillis(),
  ): DashboardSummary {
    val accountTxs = allTransactions.filter { it.accountType == accountType.value }
    val todayKey = DateFormats.formatDateKey(nowMillis)
    val (currentYear, currentMonth, _) = DateFormats.extractYmd(nowMillis)

    var todayIncome = 0.0
    var todayExpense = 0.0
    var monthIncome = 0.0
    var monthExpense = 0.0
    var totalIncome = 0.0
    var totalExpense = 0.0

    accountTxs.forEach { tx ->
      if (tx.isIncome) {
        totalIncome += tx.amount
        if (tx.year == currentYear && tx.month == currentMonth) monthIncome += tx.amount
        if (tx.dateKey == todayKey) todayIncome += tx.amount
      } else {
        totalExpense += tx.amount
        if (tx.year == currentYear && tx.month == currentMonth) monthExpense += tx.amount
        if (tx.dateKey == todayKey) todayExpense += tx.amount
      }
    }

    // Daily Expense Chart (this month)
    val daysInMonth = DateFormats.daysInMonth(currentYear, currentMonth)
    val dailyTotals = DoubleArray(daysInMonth + 1)
    accountTxs
      .filter { it.isExpense && it.year == currentYear && it.month == currentMonth }
      .forEach { tx ->
        if (tx.day in 1..daysInMonth) {
          dailyTotals[tx.day] += tx.amount
        }
      }
    val dailyBars = (1..daysInMonth).map { day -> day to dailyTotals[day] }

    // Monthly Income vs Expense (last 6 months)
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val sixMonths = mutableListOf<MonthlyBarPoint>()
    for (offset in 5 downTo 0) {
      val mCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
      val y = mCal.get(Calendar.YEAR)
      val m = mCal.get(Calendar.MONTH) + 1
      val mInc = accountTxs.filter { it.isIncome && it.year == y && it.month == m }.sumOf { it.amount }
      val mExp = accountTxs.filter { it.isExpense && it.year == y && it.month == m }.sumOf { it.amount }
      sixMonths += MonthlyBarPoint(y, m, DateFormats.shortMonthLabel(m), mInc, mExp)
    }

    // Category-wise Expense Pie Chart (current month)
    val monthExpenses = accountTxs.filter { it.isExpense && it.year == currentYear && it.month == currentMonth }
    val categorySlices = buildCategorySlices(monthExpenses)

    // Last 7 Days Spending Trend
    val trendPoints = mutableListOf<DailyTrendPoint>()
    for (daysAgo in 6 downTo 0) {
      val millis = DateFormats.daysAgoMillis(daysAgo)
      val key = DateFormats.formatDateKey(millis)
      val shortLabel = key.substring(0, 5) // DD-MM
      val spent = accountTxs.filter { it.isExpense && it.dateKey == key }.sumOf { it.amount }
      trendPoints += DailyTrendPoint(dateKey = key, label = shortLabel, amount = spent)
    }

    return DashboardSummary(
      todayIncome = todayIncome,
      todayExpense = todayExpense,
      todayBalance = todayIncome - todayExpense,
      monthIncome = monthIncome,
      monthExpense = monthExpense,
      monthSavings = monthIncome - monthExpense,
      totalAvailableBalance = totalIncome - totalExpense,
      dailyExpenseBars = dailyBars,
      monthlyComparisonBars = sixMonths,
      categorySlices = categorySlices,
      last7DaysTrend = trendPoints,
    )
  }

  fun computeMonthlyReport(
    allTransactions: List<TransactionRecord>,
    accountType: AccountType,
    year: Int,
    month: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): MonthlyReportData {
    val accountTxs = allTransactions.filter { it.accountType == accountType.value }
    val monthTxs = accountTxs.filter { it.year == year && it.month == month }
    val (prevYear, prevMonth) = DateFormats.previousMonth(year, month)
    val prevTxs = accountTxs.filter { it.year == prevYear && it.month == prevMonth }

    val totalIncome = monthTxs.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = monthTxs.filter { it.isExpense }.sumOf { it.amount }
    val totalSavings = totalIncome - totalExpense

    val prevIncome = prevTxs.filter { it.isIncome }.sumOf { it.amount }
    val prevExpense = prevTxs.filter { it.isExpense }.sumOf { it.amount }
    val prevSavings = prevIncome - prevExpense

    val expenses = monthTxs.filter { it.isExpense }
    val categorySlices = buildCategorySlices(expenses)
    val highestCategory = categorySlices.firstOrNull()

    val (curY, curM, curD) = DateFormats.extractYmd(nowMillis)
    val totalDaysInMonth = DateFormats.daysInMonth(year, month)
    val divisorDays =
      if (year == curY && month == curM) curD.coerceAtLeast(1)
      else totalDaysInMonth.coerceAtLeast(1)
    val averageDailyExpense = totalExpense / divisorDays

    val dailyBreakdown =
      (1..totalDaysInMonth)
        .mapNotNull { day ->
          val dayTxs = monthTxs.filter { it.day == day }
          if (dayTxs.isEmpty()) null
          else {
            val inc = dayTxs.filter { it.isIncome }.sumOf { it.amount }
            val exp = dayTxs.filter { it.isExpense }.sumOf { it.amount }
            val dateKey = String.format("%02d-%02d-%04d", day, month, year)
            DailyBreakdownItem(day = day, dateKey = dateKey, income = inc, expense = exp)
          }
        }
        .sortedByDescending { it.day }

    return MonthlyReportData(
      year = year,
      month = month,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      totalSavings = totalSavings,
      highestCategory = highestCategory,
      averageDailyExpense = averageDailyExpense,
      dailyBreakdown = dailyBreakdown,
      categorySlices = categorySlices,
      prevIncome = prevIncome,
      prevExpense = prevExpense,
      prevSavings = prevSavings,
      incomeDiff = totalIncome - prevIncome,
      expenseDiff = totalExpense - prevExpense,
      savingsDiff = totalSavings - prevSavings,
    )
  }

  fun computeBudgetStatuses(
    budgets: List<BudgetRecord>,
    transactions: List<TransactionRecord>,
    accountType: AccountType,
    year: Int,
    month: Int,
  ): List<BudgetStatus> {
    val monthExpenses =
      transactions.filter {
        it.accountType == accountType.value &&
          it.type == TransactionType.EXPENSE.value &&
          it.year == year &&
          it.month == month
      }

    return budgets
      .filter { it.accountType == accountType.value && it.year == year && it.month == month }
      .map { budget ->
        val spent =
          if (budget.category == BudgetRecord.OVERALL_CATEGORY) {
            monthExpenses.sumOf { it.amount }
          } else {
            monthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
          }
        val ratio = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
        val percent = (ratio * 100.0).toInt()
        val level =
          when {
            percent >= 100 -> BudgetAlertLevel.EXCEEDED_100
            percent >= 80 -> BudgetAlertLevel.WARNING_80
            else -> BudgetAlertLevel.NORMAL
          }
        BudgetStatus(
          budget = budget,
          spent = spent,
          remaining = budget.limitAmount - spent,
          percentUsed = percent,
          progressFraction = ratio.toFloat().coerceIn(0f, 1f),
          alertLevel = level,
        )
      }
      .sortedByDescending { it.percentUsed }
  }

  private fun buildCategorySlices(expenses: List<TransactionRecord>): List<CategorySlice> {
    val total = expenses.sumOf { it.amount }
    if (total <= 0.0) return emptyList()
    return expenses
      .groupBy { it.category }
      .map { (cat, list) ->
        val sum = list.sumOf { it.amount }
        CategorySlice(
          category = cat,
          amount = sum,
          percentage = ((sum / total) * 100.0).toFloat(),
        )
      }
      .sortedByDescending { it.amount }
  }
}
