package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountType
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HisabRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addUpdateDeleteTransaction_authenticatedOwner_succeedsAndEmits() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = HisabRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfile("Alice", ALICE_EMAIL)
    }

    val txId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addTransaction(
          TransactionRecord(
            accountType = AccountType.PERSONAL.value,
            type = TransactionType.EXPENSE.value,
            amount = 350.0,
            category = "খাবার",
            paymentMethod = PaymentMethod.BKASH.value,
            dateMillis = System.currentTimeMillis(),
            note = "সকালের নাস্তা",
            clientRequestId = "req_${UUID.randomUUID().toString().replace("-", "")}",
          )
        )
      }
    assertTrue(txId.isNotBlank())

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first { list -> list.any { it.id == txId } }
      }
    val created = observed.first { it.id == txId }
    assertEquals(350.0, created.amount, 0.001)
    assertEquals("খাবার", created.category)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateTransaction(created.copy(amount = 420.0, note = "দুপুরের খাবার"))
    }
    val updatedList =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first { list -> list.any { it.id == txId && it.amount == 420.0 } }
      }
    assertEquals("দুপুরের খাবার", updatedList.first { it.id == txId }.note)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteTransaction(txId)
    }
    val afterDelete =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first { list -> list.none { it.id == txId } }
      }
    assertTrue(afterDelete.none { it.id == txId })
  }

  @Test
  fun budgetsCategoriesSavingsGoals_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = HisabRepository(firestore, auth)

    val catId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addCustomCategory("অফিস ভাড়া", TransactionType.EXPENSE.value)
      }
    val categories =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCustomCategories(uid).first { list -> list.any { it.id == catId } }
      }
    assertTrue(categories.any { it.name == "অফিস ভাড়া" })

    val budgetId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.upsertBudget(
          existingId = null,
          accountType = AccountType.BUSINESS.value,
          year = 2026,
          month = 11,
          category = "ALL",
          limitAmount = 50000.0,
        )
      }
    val budgets =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeBudgets(uid).first { list -> list.any { it.id == budgetId } }
      }
    assertEquals(50000.0, budgets.first { it.id == budgetId }.limitAmount, 0.001)

    val goalId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.upsertSavingsGoal(
          SavingsGoalRecord(
            accountType = AccountType.PERSONAL.value,
            name = "জরুরি তহবিল",
            targetAmount = 100000.0,
            savedAmount = 25000.0,
            targetDateMillis = System.currentTimeMillis() + 86_400_000L * 30,
          )
        )
      }
    val goals =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSavingsGoals(uid).first { list -> list.any { it.id == goalId } }
      }
    assertEquals(25, goals.first { it.id == goalId }.progressPercent)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteCustomCategory(catId)
      repo.deleteBudget(budgetId)
      repo.deleteSavingsGoal(goalId)
    }
  }

  @Test
  fun observeTransactions_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    signInTestUser(BOB_EMAIL)
    val bobRepo = HisabRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeTransactions(aliceUid).first()
      }
      fail("Expected cross-user transaction observation to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = HisabRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions("some_user_123").first()
      }
      fail("Expected unauthenticated transaction observation to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_hisab@test.com"
    const val BOB_EMAIL = "bob_hisab@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
