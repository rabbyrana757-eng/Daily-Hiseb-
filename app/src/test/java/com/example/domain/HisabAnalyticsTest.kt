package com.example.domain

import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.util.DateFormats
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HisabAnalyticsTest {

  @Test
  fun dashboardAndBudgetAlerts_calculateAccuratelyPerAccount() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 13, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val nowMillis = cal.timeInMillis
    val todayKey = DateFormats.formatDateKey(nowMillis)

    val earlierThisMonth = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 5) }.timeInMillis
    val earlierKey = DateFormats.formatDateKey(earlierThisMonth)

    val prevMonth = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }.timeInMillis
    val prevKey = DateFormats.formatDateKey(prevMonth)

    val txs = listOf(
      TransactionRecord(
        id = "1",
        accountType = AccountType.PERSONAL.value,
        type = TransactionType.INCOME.value,
        amount = 50000.0,
        category = "চাকরির বেতন",
        dateMillis = earlierThisMonth,
        dateKey = earlierKey,
        year = 2026,
        month = 11,
        day = 5,
      ),
      TransactionRecord(
        id = "2",
        accountType = AccountType.PERSONAL.value,
        type = TransactionType.EXPENSE.value,
        amount = 8500.0,
        category = "খাবার",
        dateMillis = nowMillis,
        dateKey = todayKey,
        year = 2026,
        month = 11,
        day = 13,
      ),
      TransactionRecord(
        id = "3",
        accountType = AccountType.BUSINESS.value,
        type = TransactionType.EXPENSE.value,
        amount = 20000.0,
        category = "অন্যান্য",
        dateMillis = nowMillis,
        dateKey = todayKey,
        year = 2026,
        month = 11,
        day = 13,
      ),
      TransactionRecord(
        id = "4",
        accountType = AccountType.PERSONAL.value,
        type = TransactionType.EXPENSE.value,
        amount = 5000.0,
        category = "বাজার",
        dateMillis = prevMonth,
        dateKey = prevKey,
        year = 2026,
        month = 10,
        day = 13,
      ),
    )

    val dashboard = HisabAnalytics.computeDashboard(txs, AccountType.PERSONAL, nowMillis)
    assertEquals(0.0, dashboard.todayIncome, 0.001)
    assertEquals(8500.0, dashboard.todayExpense, 0.001)
    assertEquals(-8500.0, dashboard.todayBalance, 0.001)
    assertEquals(50000.0, dashboard.monthIncome, 0.001)
    assertEquals(8500.0, dashboard.monthExpense, 0.001)
    assertEquals(41500.0, dashboard.monthSavings, 0.001)
    assertEquals(36500.0, dashboard.totalAvailableBalance, 0.001)

    val budgets = listOf(
      BudgetRecord(
        id = "b1",
        accountType = AccountType.PERSONAL.value,
        year = 2026,
        month = 11,
        category = "খাবার",
        limitAmount = 10000.0,
      ),
      BudgetRecord(
        id = "b2",
        accountType = AccountType.PERSONAL.value,
        year = 2026,
        month = 11,
        category = BudgetRecord.OVERALL_CATEGORY,
        limitAmount = 8000.0,
      ),
    )

    val statuses = HisabAnalytics.computeBudgetStatuses(budgets, txs, AccountType.PERSONAL, 2026, 11)
    assertEquals(2, statuses.size)
    assertEquals(BudgetAlertLevel.EXCEEDED_100, statuses[0].alertLevel)
    assertEquals(106, statuses[0].percentUsed)
    assertEquals(BudgetAlertLevel.WARNING_80, statuses[1].alertLevel)
    assertEquals(85, statuses[1].percentUsed)

    val report = HisabAnalytics.computeMonthlyReport(txs, AccountType.PERSONAL, 2026, 11, nowMillis)
    assertNotNull(report.highestCategory)
    assertEquals("খাবার", report.highestCategory?.category)
    assertEquals(5000.0, report.prevExpense, 0.001)
    assertEquals(3500.0, report.expenseDiff, 0.001)
  }
}
