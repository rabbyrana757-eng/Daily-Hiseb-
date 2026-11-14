package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.awaitTask
import com.example.data.remote.handleFirestoreError
import com.example.util.DateFormats
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class HisabRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  private fun transactionsCol(uid: String) = userDoc(uid).collection("transactions")

  private fun categoriesCol(uid: String) = userDoc(uid).collection("categories")

  private fun budgetsCol(uid: String) = userDoc(uid).collection("budgets")

  private fun savingsCol(uid: String) = userDoc(uid).collection("savingsGoals")

  // --- User Profile ---

  suspend fun ensureUserProfile(displayName: String?, email: String?) {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    try {
      val snapshot = docRef.get().awaitTask()
      if (!snapshot.exists()) {
        val payload = buildMap<String, Any> {
          put("userId", uid)
          if (!displayName.isNullOrBlank()) put("displayName", displayName.take(100))
          if (!email.isNullOrBlank()) put("email", email.take(150))
          put("activeAccount", AccountType.PERSONAL.value)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).awaitTask()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val docRef = userDoc(uid)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleFlowError(error, OperationType.GET, docRef.path, uid)
        }
    )
  }

  suspend fun updateActiveAccount(accountType: AccountType) {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    try {
      val snapshot = docRef.get().awaitTask()
      if (snapshot.exists()) {
        docRef.update(
          mapOf(
            "activeAccount" to accountType.value,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).awaitTask()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "activeAccount" to accountType.value,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).awaitTask()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  // --- Transactions ---

  fun observeTransactions(uid: String): Flow<List<TransactionRecord>> = flow {
    val path = "users/$uid/transactions"
    emitAll(
      transactionsCol(uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              TransactionRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedWith(
              compareByDescending<TransactionRecord> { it.dateMillis }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
        }
    )
  }

  suspend fun getTransactionsOnce(): List<TransactionRecord> {
    val uid = requireUserId()
    val col = transactionsCol(uid)
    return try {
      col
        .get()
        .awaitTask()
        .toObjects(
          TransactionRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .sortedByDescending { it.dateMillis }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, col.path, auth)
      throw e
    }
  }

  suspend fun addTransaction(record: TransactionRecord): String {
    val uid = requireUserId()
    require(record.amount > 0.0) { "Amount must be greater than zero" }
    require(record.category.isNotBlank()) { "Category cannot be blank" }

    val (year, month, day) = DateFormats.extractYmd(record.dateMillis)
    val dateKey = DateFormats.formatDateKey(record.dateMillis)
    // Use clientRequestId as deterministic document id when provided to prevent duplicate writes
    val docRef =
      if (!record.clientRequestId.isNullOrBlank()) {
        transactionsCol(uid).document(record.clientRequestId.take(120))
      } else {
        transactionsCol(uid).document()
      }

    val payload = buildMap<String, Any> {
      put("userId", uid)
      put("accountType", record.accountType)
      put("type", record.type)
      put("amount", record.amount)
      put("category", record.category.trim().take(60))
      put("paymentMethod", record.paymentMethod)
      put("dateMillis", record.dateMillis)
      put("dateKey", dateKey)
      put("year", year)
      put("month", month)
      put("day", day)
      if (!record.note.isNullOrBlank()) put("note", record.note.trim().take(300))
      if (!record.receiptBase64.isNullOrBlank()) {
        put("receiptBase64", record.receiptBase64.take(200_000))
      }
      if (!record.clientRequestId.isNullOrBlank()) {
        put("clientRequestId", record.clientRequestId.take(80))
      }
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    try {
      docRef.set(payload).awaitTask()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateTransaction(record: TransactionRecord) {
    val uid = requireUserId()
    require(record.id.isNotBlank()) { "Transaction ID must not be blank for update" }
    require(record.amount > 0.0) { "Amount must be greater than zero" }

    val (year, month, day) = DateFormats.extractYmd(record.dateMillis)
    val dateKey = DateFormats.formatDateKey(record.dateMillis)
    val docRef = transactionsCol(uid).document(record.id)

    val updates = buildMap<String, Any> {
      put("accountType", record.accountType)
      put("type", record.type)
      put("amount", record.amount)
      put("category", record.category.trim().take(60))
      put("paymentMethod", record.paymentMethod)
      put("dateMillis", record.dateMillis)
      put("dateKey", dateKey)
      put("year", year)
      put("month", month)
      put("day", day)
      put("note", record.note?.trim()?.take(300) ?: "")
      if (!record.receiptBase64.isNullOrBlank()) {
        put("receiptBase64", record.receiptBase64.take(200_000))
      } else {
        put("receiptBase64", FieldValue.delete())
      }
      put("updatedAt", FieldValue.serverTimestamp())
    }

    try {
      docRef.update(updates).awaitTask()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteTransaction(transactionId: String) {
    val uid = requireUserId()
    require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
    val docRef = transactionsCol(uid).document(transactionId)
    try {
      docRef.delete().awaitTask()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  // --- Custom Categories ---

  fun observeCustomCategories(uid: String): Flow<List<CategoryRecord>> = flow {
    val path = "users/$uid/categories"
    emitAll(
      categoriesCol(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CategoryRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
        }
    )
  }

  suspend fun addCustomCategory(name: String, type: String): String {
    val uid = requireUserId()
    val trimmed = name.trim().take(60)
    require(trimmed.isNotEmpty()) { "Category name cannot be empty" }
    val docRef = categoriesCol(uid).document()
    val payload =
      mapOf(
        "userId" to uid,
        "name" to trimmed,
        "type" to type,
        "isCustom" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).awaitTask()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteCustomCategory(categoryId: String) {
    val uid = requireUserId()
    val docRef = categoriesCol(uid).document(categoryId)
    try {
      docRef.delete().awaitTask()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  // --- Budgets ---

  fun observeBudgets(uid: String): Flow<List<BudgetRecord>> = flow {
    val path = "users/$uid/budgets"
    emitAll(
      budgetsCol(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            BudgetRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
        }
    )
  }

  suspend fun upsertBudget(
    existingId: String?,
    accountType: String,
    year: Int,
    month: Int,
    category: String,
    limitAmount: Double,
  ): String {
    val uid = requireUserId()
    require(limitAmount > 0.0) { "Budget limit must be positive" }
    val docRef =
      if (!existingId.isNullOrBlank()) budgetsCol(uid).document(existingId)
      else budgetsCol(uid).document()

    try {
      if (!existingId.isNullOrBlank()) {
        docRef
          .update(
            mapOf(
              "accountType" to accountType,
              "year" to year,
              "month" to month,
              "category" to category.trim().take(60),
              "limitAmount" to limitAmount,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .awaitTask()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "accountType" to accountType,
              "year" to year,
              "month" to month,
              "category" to category.trim().take(60),
              "limitAmount" to limitAmount,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .awaitTask()
      }
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
        auth,
      )
      throw e
    }
  }

  suspend fun deleteBudget(budgetId: String) {
    val uid = requireUserId()
    val docRef = budgetsCol(uid).document(budgetId)
    try {
      docRef.delete().awaitTask()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  // --- Savings Goals ---

  fun observeSavingsGoals(uid: String): Flow<List<SavingsGoalRecord>> = flow {
    val path = "users/$uid/savingsGoals"
    emitAll(
      savingsCol(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SavingsGoalRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
        }
    )
  }

  suspend fun upsertSavingsGoal(goal: SavingsGoalRecord): String {
    val uid = requireUserId()
    require(goal.name.isNotBlank()) { "Goal name cannot be blank" }
    require(goal.targetAmount > 0.0) { "Target amount must be positive" }
    require(goal.savedAmount >= 0.0) { "Saved amount cannot be negative" }

    val targetDateKey = DateFormats.formatDateKey(goal.targetDateMillis)
    val isUpdate = goal.id.isNotBlank()
    val docRef = if (isUpdate) savingsCol(uid).document(goal.id) else savingsCol(uid).document()

    try {
      if (isUpdate) {
        docRef
          .update(
            mapOf(
              "accountType" to goal.accountType,
              "name" to goal.name.trim().take(80),
              "targetAmount" to goal.targetAmount,
              "savedAmount" to goal.savedAmount,
              "targetDateMillis" to goal.targetDateMillis,
              "targetDateKey" to targetDateKey,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .awaitTask()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "accountType" to goal.accountType,
              "name" to goal.name.trim().take(80),
              "targetAmount" to goal.targetAmount,
              "savedAmount" to goal.savedAmount,
              "targetDateMillis" to goal.targetDateMillis,
              "targetDateKey" to targetDateKey,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .awaitTask()
      }
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
        auth,
      )
      throw e
    }
  }

  suspend fun deleteSavingsGoal(goalId: String) {
    val uid = requireUserId()
    val docRef = savingsCol(uid).document(goalId)
    try {
      docRef.delete().awaitTask()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  // --- Backup & Restore ---

  suspend fun restoreBackup(
    transactions: List<TransactionRecord>,
    categories: List<CategoryRecord>,
    budgets: List<BudgetRecord>,
    goals: List<SavingsGoalRecord>,
  ) {
    val uid = requireUserId()
    transactions.forEach { tx ->
      addTransaction(
        tx.copy(
          id = "",
          userId = uid,
          clientRequestId = tx.clientRequestId ?: tx.id.takeIf { it.isNotBlank() },
        )
      )
    }
    categories.forEach { cat ->
      addCustomCategory(cat.name, cat.type)
    }
    budgets.forEach { budget ->
      upsertBudget(
        existingId = null,
        accountType = budget.accountType,
        year = budget.year,
        month = budget.month,
        category = budget.category,
        limitAmount = budget.limitAmount,
      )
    }
    goals.forEach { goal ->
      upsertSavingsGoal(goal.copy(id = "", userId = uid))
    }
  }

  private fun handleFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
    throw error
  }
}
