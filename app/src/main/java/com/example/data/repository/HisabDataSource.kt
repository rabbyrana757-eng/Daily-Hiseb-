package com.example.data.repository

import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyRecord
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

interface HisabDataSource {
  suspend fun ensureUserProfile(displayName: String?, email: String?)

  suspend fun updateActiveAccount(accountType: AccountType)

  fun observeTransactions(uid: String): Flow<List<TransactionRecord>>

  suspend fun addTransaction(record: TransactionRecord): String

  suspend fun updateTransaction(record: TransactionRecord)

  suspend fun deleteTransaction(transactionId: String)

  fun observeCustomCategories(uid: String): Flow<List<CategoryRecord>>

  suspend fun addCustomCategory(name: String, type: String): String

  suspend fun deleteCustomCategory(categoryId: String)

  fun observeBudgets(uid: String): Flow<List<BudgetRecord>>

  suspend fun upsertBudget(
    existingId: String?,
    accountType: String,
    year: Int,
    month: Int,
    category: String,
    limitAmount: Double,
  ): String

  suspend fun deleteBudget(budgetId: String)

  fun observeSavingsGoals(uid: String): Flow<List<SavingsGoalRecord>>

  suspend fun upsertSavingsGoal(goal: SavingsGoalRecord): String

  suspend fun deleteSavingsGoal(goalId: String)

  fun observeParties(uid: String): Flow<List<PartyRecord>>

  suspend fun upsertParty(party: PartyRecord): String

  suspend fun deleteParty(partyId: String)

  fun observePartyEntries(uid: String): Flow<List<PartyEntryRecord>>

  suspend fun addPartyEntry(entry: PartyEntryRecord): String

  suspend fun updatePartyEntry(entry: PartyEntryRecord)

  suspend fun deletePartyEntry(entryId: String)

  suspend fun restoreBackup(
    transactions: List<TransactionRecord>,
    categories: List<CategoryRecord>,
    budgets: List<BudgetRecord>,
    goals: List<SavingsGoalRecord>,
    parties: List<PartyRecord> = emptyList(),
    partyEntries: List<PartyEntryRecord> = emptyList(),
  )
}
