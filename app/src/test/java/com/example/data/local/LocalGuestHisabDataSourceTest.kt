package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.domain.HisabAnalytics
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalGuestHisabDataSourceTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `guest data persists across instances and supports CRUD`() = runTest {
    val store = LocalGuestHisabDataSource(context)
    store.clearAllData()

    val txId = store.addTransaction(
      TransactionRecord(
        accountType = AccountType.PERSONAL.value,
        type = TransactionType.EXPENSE.value,
        amount = 450.0,
        category = "বাজার",
        paymentMethod = PaymentMethod.BKASH.value,
        dateMillis = 1767225600000L,
        note = "সকালের বাজার",
        clientRequestId = "req-1",
      )
    )
    assertEquals("req-1", txId)

    // Duplicate clientRequestId should not create a second entry
    store.addTransaction(
      TransactionRecord(
        accountType = AccountType.PERSONAL.value,
        type = TransactionType.EXPENSE.value,
        amount = 450.0,
        category = "বাজার",
        paymentMethod = PaymentMethod.BKASH.value,
        dateMillis = 1767225600000L,
        clientRequestId = "req-1",
      )
    )

    val catId = store.addCustomCategory("ফ্রিল্যান্স", TransactionType.INCOME.value)
    val budgetId = store.upsertBudget(
      existingId = null,
      accountType = AccountType.PERSONAL.value,
      year = 2026,
      month = 1,
      category = BudgetRecord.OVERALL_CATEGORY,
      limitAmount = 20000.0,
    )
    val goalId = store.upsertSavingsGoal(
      SavingsGoalRecord(
        accountType = AccountType.PERSONAL.value,
        name = "ল্যাপটপ",
        targetAmount = 80000.0,
        savedAmount = 15000.0,
        targetDateMillis = 1798675200000L,
      )
    )

    // Reload from disk via a fresh instance
    val reloaded = LocalGuestHisabDataSource(context)
    val txs = reloaded.observeTransactions("guest_local").first()
    val cats = reloaded.observeCustomCategories("guest_local").first()
    val budgets = reloaded.observeBudgets("guest_local").first()
    val goals = reloaded.observeSavingsGoals("guest_local").first()

    assertEquals(1, txs.size)
    assertEquals(450.0, txs.first().amount, 0.01)
    assertEquals(1, cats.size)
    assertEquals(catId, cats.first().id)
    assertEquals(1, budgets.size)
    assertEquals(budgetId, budgets.first().id)
    assertEquals(1, goals.size)
    assertEquals(goalId, goals.first().id)

    reloaded.deleteTransaction(txId)
    reloaded.deleteCustomCategory(catId)
    reloaded.deleteBudget(budgetId)
    reloaded.deleteSavingsGoal(goalId)

    val payload = reloaded.exportPayloadOnce()
    assertTrue(payload.transactions.isEmpty())
    assertTrue(payload.categories.isEmpty())
    assertTrue(payload.budgets.isEmpty())
    assertTrue(payload.goals.isEmpty())
  }

  @Test
  fun `tally khata parties and entries persist, compute balances, and format WhatsApp phone`() = runTest {
    val store = LocalGuestHisabDataSource(context)
    store.clearAllData()

    val customerId = store.upsertParty(
      PartyRecord(
        partyType = PartyType.CUSTOMER.value,
        name = "করিম স্টোর",
        phone = "01712-345678",
        address = "মিরপুর",
      )
    )
    val supplierId = store.upsertParty(
      PartyRecord(
        partyType = PartyType.SUPPLIER.value,
        name = "আলী ট্রেডার্স",
        phone = "+8801811223344",
      )
    )

    store.addPartyEntry(
      PartyEntryRecord(
        partyId = customerId,
        entryType = PartyEntryType.GIVEN.value,
        amount = 1500.0,
        paymentMethod = PaymentMethod.CASH.value,
        dateMillis = 1767225600000L,
        note = "বাকি বিক্রি",
      )
    )
    store.addPartyEntry(
      PartyEntryRecord(
        partyId = customerId,
        entryType = PartyEntryType.RECEIVED.value,
        amount = 500.0,
        paymentMethod = PaymentMethod.BKASH.value,
        dateMillis = 1767312000000L,
        note = "নগদ জমা",
      )
    )
    store.addPartyEntry(
      PartyEntryRecord(
        partyId = supplierId,
        entryType = PartyEntryType.RECEIVED.value,
        amount = 4000.0,
        paymentMethod = PaymentMethod.BANK.value,
        dateMillis = 1767225600000L,
        note = "মাল কেনা",
      )
    )
    store.addPartyEntry(
      PartyEntryRecord(
        partyId = supplierId,
        entryType = PartyEntryType.GIVEN.value,
        amount = 1200.0,
        paymentMethod = PaymentMethod.CASH.value,
        dateMillis = 1767312000000L,
        note = "পরিশোধ",
      )
    )

    val reloaded = LocalGuestHisabDataSource(context)
    val parties = reloaded.observeParties("guest_local").first()
    val entries = reloaded.observePartyEntries("guest_local").first()
    assertEquals(2, parties.size)
    assertEquals(4, entries.size)

    val summary = HisabAnalytics.computeTallySummary(parties, entries)
    assertEquals(1000.0, summary.totalReceivable, 0.01)
    assertEquals(2800.0, summary.totalPayable, 0.01)
    assertEquals(-1800.0, summary.netBalance, 0.01)
    assertEquals(1, summary.customerCount)
    assertEquals(1, summary.supplierCount)

    assertEquals("8801712345678", WhatsAppShareHelper.normalizeWhatsAppPhone("01712-345678"))
    assertEquals("8801811223344", WhatsAppShareHelper.normalizeWhatsAppPhone("+8801811223344"))

    // Deleting party removes its entries as well
    reloaded.deleteParty(customerId)
    assertEquals(1, reloaded.observeParties("guest_local").first().size)
    assertEquals(2, reloaded.observePartyEntries("guest_local").first().size)
  }
}
