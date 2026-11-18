package com.example.data.local

import android.content.Context
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.HisabDataSource
import com.example.util.BackupPayload
import com.example.util.DateFormats
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalGuestHisabDataSource(context: Context) : HisabDataSource {

  private val appContext = context.applicationContext
  private val storageFile = File(appContext.filesDir, FILE_NAME)
  private val mutex = Mutex()
  @Volatile private var loaded = false

  private val transactionsFlow = MutableStateFlow<List<TransactionRecord>>(emptyList())
  private val categoriesFlow = MutableStateFlow<List<CategoryRecord>>(emptyList())
  private val budgetsFlow = MutableStateFlow<List<BudgetRecord>>(emptyList())
  private val savingsGoalsFlow = MutableStateFlow<List<SavingsGoalRecord>>(emptyList())
  private val partiesFlow = MutableStateFlow<List<PartyRecord>>(emptyList())
  private val partyEntriesFlow = MutableStateFlow<List<PartyEntryRecord>>(emptyList())

  private suspend fun ensureLoaded() {
    if (loaded) return
    mutex.withLock {
      if (loaded) return
      withContext(Dispatchers.IO) {
        if (storageFile.exists()) {
          runCatching {
            val text = storageFile.readText(Charsets.UTF_8)
            if (text.isNotBlank()) {
              parseFromJson(text)
            }
          }
        }
      }
      loaded = true
    }
  }

  private fun parseFromJson(jsonText: String) {
    val root = JSONObject(jsonText)

    val txList = mutableListOf<TransactionRecord>()
    val txArr = root.optJSONArray("transactions") ?: JSONArray()
    for (i in 0 until txArr.length()) {
      val obj = txArr.getJSONObject(i)
      val amount = obj.optDouble("amount", 0.0)
      if (amount <= 0.0) continue
      val dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
      val (y, m, d) = DateFormats.extractYmd(dateMillis)
      txList += TransactionRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        accountType = obj.optString("accountType", AccountType.PERSONAL.value),
        type = obj.optString("type", TransactionType.EXPENSE.value),
        amount = amount,
        category = obj.optString("category", "অন্যান্য"),
        paymentMethod = obj.optString("paymentMethod", "Cash"),
        dateMillis = dateMillis,
        dateKey = obj.optString("dateKey", "").ifBlank { DateFormats.formatDateKey(dateMillis) },
        year = obj.optInt("year", y),
        month = obj.optInt("month", m),
        day = obj.optInt("day", d),
        note = obj.optString("note", "").takeIf { it.isNotBlank() },
        receiptBase64 = obj.optString("receiptBase64", "").takeIf { it.isNotBlank() },
        clientRequestId = obj.optString("clientRequestId", "").takeIf { it.isNotBlank() },
      )
    }

    val catList = mutableListOf<CategoryRecord>()
    val catArr = root.optJSONArray("categories") ?: JSONArray()
    for (i in 0 until catArr.length()) {
      val obj = catArr.getJSONObject(i)
      val name = obj.optString("name", "")
      if (name.isBlank()) continue
      catList += CategoryRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        name = name,
        type = obj.optString("type", TransactionType.EXPENSE.value),
        isCustom = true,
      )
    }

    val budgetList = mutableListOf<BudgetRecord>()
    val bArr = root.optJSONArray("budgets") ?: JSONArray()
    for (i in 0 until bArr.length()) {
      val obj = bArr.getJSONObject(i)
      val limit = obj.optDouble("limitAmount", 0.0)
      if (limit <= 0.0) continue
      budgetList += BudgetRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        accountType = obj.optString("accountType", AccountType.PERSONAL.value),
        year = obj.optInt("year", 2026),
        month = obj.optInt("month", 1),
        category = obj.optString("category", BudgetRecord.OVERALL_CATEGORY),
        limitAmount = limit,
      )
    }

    val goalList = mutableListOf<SavingsGoalRecord>()
    val gArr = root.optJSONArray("savingsGoals") ?: JSONArray()
    for (i in 0 until gArr.length()) {
      val obj = gArr.getJSONObject(i)
      val target = obj.optDouble("targetAmount", 0.0)
      if (target <= 0.0) continue
      val targetDateMillis = obj.optLong("targetDateMillis", System.currentTimeMillis())
      goalList += SavingsGoalRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        accountType = obj.optString("accountType", AccountType.PERSONAL.value),
        name = obj.optString("name", "সঞ্চয়"),
        targetAmount = target,
        savedAmount = obj.optDouble("savedAmount", 0.0).coerceAtLeast(0.0),
        targetDateMillis = targetDateMillis,
        targetDateKey = obj.optString("targetDateKey", "")
          .ifBlank { DateFormats.formatDateKey(targetDateMillis) },
      )
    }

    val partyList = mutableListOf<PartyRecord>()
    val pArr = root.optJSONArray("parties") ?: JSONArray()
    for (i in 0 until pArr.length()) {
      val obj = pArr.getJSONObject(i)
      val name = obj.optString("name", "")
      if (name.isBlank()) continue
      partyList += PartyRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        partyType = obj.optString("partyType", PartyType.CUSTOMER.value),
        name = name,
        phone = obj.optString("phone", "").takeIf { it.isNotBlank() },
        address = obj.optString("address", "").takeIf { it.isNotBlank() },
        note = obj.optString("note", "").takeIf { it.isNotBlank() },
      )
    }

    val entryList = mutableListOf<PartyEntryRecord>()
    val eArr = root.optJSONArray("partyEntries") ?: JSONArray()
    for (i in 0 until eArr.length()) {
      val obj = eArr.getJSONObject(i)
      val partyId = obj.optString("partyId", "")
      val amount = obj.optDouble("amount", 0.0)
      if (partyId.isBlank() || amount <= 0.0) continue
      val dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
      entryList += PartyEntryRecord(
        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
        userId = GUEST_UID,
        partyId = partyId,
        entryType = obj.optString("entryType", PartyEntryType.GIVEN.value),
        amount = amount,
        paymentMethod = obj.optString("paymentMethod", "Cash"),
        dateMillis = dateMillis,
        dateKey = obj.optString("dateKey", "").ifBlank { DateFormats.formatDateKey(dateMillis) },
        note = obj.optString("note", "").takeIf { it.isNotBlank() },
        clientRequestId = obj.optString("clientRequestId", "").takeIf { it.isNotBlank() },
      )
    }

    transactionsFlow.value = txList.sortedByDescending { it.dateMillis }
    categoriesFlow.value = catList
    budgetsFlow.value = budgetList
    savingsGoalsFlow.value = goalList
    partiesFlow.value = partyList.sortedBy { it.name.lowercase() }
    partyEntriesFlow.value = entryList.sortedByDescending { it.dateMillis }
  }

  private suspend fun persistLocked() {
    withContext(Dispatchers.IO) {
      val root = JSONObject()
      root.put("version", 1)

      val txArray = JSONArray()
      transactionsFlow.value.forEach { tx ->
        txArray.put(
          JSONObject().apply {
            put("id", tx.id)
            put("accountType", tx.accountType)
            put("type", tx.type)
            put("amount", tx.amount)
            put("category", tx.category)
            put("paymentMethod", tx.paymentMethod)
            put("dateMillis", tx.dateMillis)
            put("dateKey", tx.dateKey)
            put("year", tx.year)
            put("month", tx.month)
            put("day", tx.day)
            put("note", tx.note.orEmpty())
            put("receiptBase64", tx.receiptBase64.orEmpty())
            put("clientRequestId", tx.clientRequestId.orEmpty())
          }
        )
      }
      root.put("transactions", txArray)

      val catArray = JSONArray()
      categoriesFlow.value.forEach { cat ->
        catArray.put(
          JSONObject().apply {
            put("id", cat.id)
            put("name", cat.name)
            put("type", cat.type)
          }
        )
      }
      root.put("categories", catArray)

      val budgetArray = JSONArray()
      budgetsFlow.value.forEach { b ->
        budgetArray.put(
          JSONObject().apply {
            put("id", b.id)
            put("accountType", b.accountType)
            put("year", b.year)
            put("month", b.month)
            put("category", b.category)
            put("limitAmount", b.limitAmount)
          }
        )
      }
      root.put("budgets", budgetArray)

      val goalArray = JSONArray()
      savingsGoalsFlow.value.forEach { g ->
        goalArray.put(
          JSONObject().apply {
            put("id", g.id)
            put("accountType", g.accountType)
            put("name", g.name)
            put("targetAmount", g.targetAmount)
            put("savedAmount", g.savedAmount)
            put("targetDateMillis", g.targetDateMillis)
            put("targetDateKey", g.targetDateKey)
          }
        )
      }
      root.put("savingsGoals", goalArray)

      val partyArray = JSONArray()
      partiesFlow.value.forEach { p ->
        partyArray.put(
          JSONObject().apply {
            put("id", p.id)
            put("partyType", p.partyType)
            put("name", p.name)
            put("phone", p.phone.orEmpty())
            put("address", p.address.orEmpty())
            put("note", p.note.orEmpty())
          }
        )
      }
      root.put("parties", partyArray)

      val entryArray = JSONArray()
      partyEntriesFlow.value.forEach { e ->
        entryArray.put(
          JSONObject().apply {
            put("id", e.id)
            put("partyId", e.partyId)
            put("entryType", e.entryType)
            put("amount", e.amount)
            put("paymentMethod", e.paymentMethod)
            put("dateMillis", e.dateMillis)
            put("dateKey", e.dateKey)
            put("note", e.note.orEmpty())
            put("clientRequestId", e.clientRequestId.orEmpty())
          }
        )
      }
      root.put("partyEntries", entryArray)

      val tempFile = File(appContext.filesDir, "$FILE_NAME.tmp")
      tempFile.writeText(root.toString(), Charsets.UTF_8)
      if (!tempFile.renameTo(storageFile)) {
        tempFile.copyTo(storageFile, overwrite = true)
        tempFile.delete()
      }
    }
  }

  override suspend fun ensureUserProfile(displayName: String?, email: String?) {
    ensureLoaded()
  }

  override suspend fun updateActiveAccount(accountType: AccountType) {
    // Active account preference is already persisted in AppPreferences DataStore
  }

  override fun observeTransactions(uid: String): Flow<List<TransactionRecord>> = flow {
    ensureLoaded()
    emitAll(transactionsFlow)
  }

  override suspend fun addTransaction(record: TransactionRecord): String {
    require(record.amount > 0.0) { "Amount must be greater than zero" }
    require(record.category.isNotBlank()) { "Category cannot be blank" }
    ensureLoaded()

    val (year, month, day) = DateFormats.extractYmd(record.dateMillis)
    val dateKey = DateFormats.formatDateKey(record.dateMillis)
    val docId =
      record.clientRequestId?.takeIf { it.isNotBlank() }?.take(120)
        ?: UUID.randomUUID().toString()

    val newRecord = TransactionRecord(
      id = docId,
      userId = GUEST_UID,
      accountType = record.accountType,
      type = record.type,
      amount = record.amount,
      category = record.category.trim().take(60),
      paymentMethod = record.paymentMethod,
      dateMillis = record.dateMillis,
      dateKey = dateKey,
      year = year,
      month = month,
      day = day,
      note = record.note?.trim()?.take(300)?.takeIf { it.isNotBlank() },
      receiptBase64 = record.receiptBase64?.takeIf { it.isNotBlank() }?.take(200_000),
      clientRequestId = record.clientRequestId?.takeIf { it.isNotBlank() }?.take(80),
    )

    mutex.withLock {
      val updated = transactionsFlow.value.filterNot { it.id == docId } + newRecord
      transactionsFlow.value = updated.sortedByDescending { it.dateMillis }
      persistLocked()
    }
    return docId
  }

  override suspend fun updateTransaction(record: TransactionRecord) {
    require(record.id.isNotBlank()) { "Transaction ID must not be blank for update" }
    require(record.amount > 0.0) { "Amount must be greater than zero" }
    ensureLoaded()

    val (year, month, day) = DateFormats.extractYmd(record.dateMillis)
    val dateKey = DateFormats.formatDateKey(record.dateMillis)

    mutex.withLock {
      val updated = transactionsFlow.value.map { existing ->
        if (existing.id == record.id) {
          existing.copy(
            accountType = record.accountType,
            type = record.type,
            amount = record.amount,
            category = record.category.trim().take(60),
            paymentMethod = record.paymentMethod,
            dateMillis = record.dateMillis,
            dateKey = dateKey,
            year = year,
            month = month,
            day = day,
            note = record.note?.trim()?.take(300)?.takeIf { it.isNotBlank() },
            receiptBase64 = record.receiptBase64?.takeIf { it.isNotBlank() }?.take(200_000),
          )
        } else {
          existing
        }
      }
      transactionsFlow.value = updated.sortedByDescending { it.dateMillis }
      persistLocked()
    }
  }

  override suspend fun deleteTransaction(transactionId: String) {
    require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
    ensureLoaded()
    mutex.withLock {
      transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transactionId }
      persistLocked()
    }
  }

  override fun observeCustomCategories(uid: String): Flow<List<CategoryRecord>> = flow {
    ensureLoaded()
    emitAll(categoriesFlow)
  }

  override suspend fun addCustomCategory(name: String, type: String): String {
    val trimmed = name.trim().take(60)
    require(trimmed.isNotEmpty()) { "Category name cannot be empty" }
    ensureLoaded()

    val newId = UUID.randomUUID().toString()
    val newCategory = CategoryRecord(
      id = newId,
      userId = GUEST_UID,
      name = trimmed,
      type = type,
      isCustom = true,
    )
    mutex.withLock {
      categoriesFlow.value = categoriesFlow.value + newCategory
      persistLocked()
    }
    return newId
  }

  override suspend fun deleteCustomCategory(categoryId: String) {
    ensureLoaded()
    mutex.withLock {
      categoriesFlow.value = categoriesFlow.value.filterNot { it.id == categoryId }
      persistLocked()
    }
  }

  override fun observeBudgets(uid: String): Flow<List<BudgetRecord>> = flow {
    ensureLoaded()
    emitAll(budgetsFlow)
  }

  override suspend fun upsertBudget(
    existingId: String?,
    accountType: String,
    year: Int,
    month: Int,
    category: String,
    limitAmount: Double,
  ): String {
    require(limitAmount > 0.0) { "Budget limit must be positive" }
    ensureLoaded()

    val id = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    val record = BudgetRecord(
      id = id,
      userId = GUEST_UID,
      accountType = accountType,
      year = year,
      month = month,
      category = category.trim().take(60),
      limitAmount = limitAmount,
    )
    mutex.withLock {
      val list = budgetsFlow.value
      budgetsFlow.value =
        if (list.any { it.id == id }) {
          list.map { if (it.id == id) record else it }
        } else {
          list + record
        }
      persistLocked()
    }
    return id
  }

  override suspend fun deleteBudget(budgetId: String) {
    ensureLoaded()
    mutex.withLock {
      budgetsFlow.value = budgetsFlow.value.filterNot { it.id == budgetId }
      persistLocked()
    }
  }

  override fun observeSavingsGoals(uid: String): Flow<List<SavingsGoalRecord>> = flow {
    ensureLoaded()
    emitAll(savingsGoalsFlow)
  }

  override suspend fun upsertSavingsGoal(goal: SavingsGoalRecord): String {
    require(goal.name.isNotBlank()) { "Goal name cannot be blank" }
    require(goal.targetAmount > 0.0) { "Target amount must be positive" }
    require(goal.savedAmount >= 0.0) { "Saved amount cannot be negative" }
    ensureLoaded()

    val id = goal.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    val record = goal.copy(
      id = id,
      userId = GUEST_UID,
      name = goal.name.trim().take(80),
      targetDateKey = DateFormats.formatDateKey(goal.targetDateMillis),
    )
    mutex.withLock {
      val list = savingsGoalsFlow.value
      savingsGoalsFlow.value =
        if (list.any { it.id == id }) {
          list.map { if (it.id == id) record else it }
        } else {
          list + record
        }
      persistLocked()
    }
    return id
  }

  override suspend fun deleteSavingsGoal(goalId: String) {
    ensureLoaded()
    mutex.withLock {
      savingsGoalsFlow.value = savingsGoalsFlow.value.filterNot { it.id == goalId }
      persistLocked()
    }
  }

  override fun observeParties(uid: String): Flow<List<PartyRecord>> = flow {
    ensureLoaded()
    emitAll(partiesFlow)
  }

  override suspend fun upsertParty(party: PartyRecord): String {
    val cleanName = party.name.trim().take(100)
    require(cleanName.isNotEmpty()) { "Party name cannot be empty" }
    ensureLoaded()

    val id = party.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    val record = party.copy(
      id = id,
      userId = GUEST_UID,
      name = cleanName,
      phone = party.phone?.trim()?.take(30)?.takeIf { it.isNotBlank() },
      address = party.address?.trim()?.take(200)?.takeIf { it.isNotBlank() },
      note = party.note?.trim()?.take(300)?.takeIf { it.isNotBlank() },
    )
    mutex.withLock {
      val list = partiesFlow.value
      val next =
        if (list.any { it.id == id }) list.map { if (it.id == id) record else it }
        else list + record
      partiesFlow.value = next.sortedBy { it.name.lowercase() }
      persistLocked()
    }
    return id
  }

  override suspend fun deleteParty(partyId: String) {
    ensureLoaded()
    mutex.withLock {
      partiesFlow.value = partiesFlow.value.filterNot { it.id == partyId }
      partyEntriesFlow.value = partyEntriesFlow.value.filterNot { it.partyId == partyId }
      persistLocked()
    }
  }

  override fun observePartyEntries(uid: String): Flow<List<PartyEntryRecord>> = flow {
    ensureLoaded()
    emitAll(partyEntriesFlow)
  }

  override suspend fun addPartyEntry(entry: PartyEntryRecord): String {
    require(entry.partyId.isNotBlank()) { "Party ID cannot be blank" }
    require(entry.amount > 0.0) { "Amount must be greater than zero" }
    ensureLoaded()

    val docId =
      entry.clientRequestId?.takeIf { it.isNotBlank() }?.take(120)
        ?: UUID.randomUUID().toString()
    val record = entry.copy(
      id = docId,
      userId = GUEST_UID,
      dateKey = DateFormats.formatDateKey(entry.dateMillis),
      note = entry.note?.trim()?.take(300)?.takeIf { it.isNotBlank() },
      clientRequestId = entry.clientRequestId?.takeIf { it.isNotBlank() }?.take(80),
    )
    mutex.withLock {
      val updated = partyEntriesFlow.value.filterNot { it.id == docId } + record
      partyEntriesFlow.value = updated.sortedByDescending { it.dateMillis }
      persistLocked()
    }
    return docId
  }

  override suspend fun updatePartyEntry(entry: PartyEntryRecord) {
    require(entry.id.isNotBlank()) { "Entry ID must not be blank" }
    require(entry.partyId.isNotBlank()) { "Party ID must not be blank" }
    require(entry.amount > 0.0) { "Amount must be greater than zero" }
    ensureLoaded()

    val dateKey = DateFormats.formatDateKey(entry.dateMillis)
    mutex.withLock {
      partyEntriesFlow.value =
        partyEntriesFlow.value
          .map { existing ->
            if (existing.id == entry.id) {
              existing.copy(
                partyId = entry.partyId,
                entryType = entry.entryType,
                amount = entry.amount,
                paymentMethod = entry.paymentMethod,
                dateMillis = entry.dateMillis,
                dateKey = dateKey,
                note = entry.note?.trim()?.take(300)?.takeIf { it.isNotBlank() },
              )
            } else {
              existing
            }
          }
          .sortedByDescending { it.dateMillis }
      persistLocked()
    }
  }

  override suspend fun deletePartyEntry(entryId: String) {
    ensureLoaded()
    mutex.withLock {
      partyEntriesFlow.value = partyEntriesFlow.value.filterNot { it.id == entryId }
      persistLocked()
    }
  }

  override suspend fun restoreBackup(
    transactions: List<TransactionRecord>,
    categories: List<CategoryRecord>,
    budgets: List<BudgetRecord>,
    goals: List<SavingsGoalRecord>,
    parties: List<PartyRecord>,
    partyEntries: List<PartyEntryRecord>,
  ) {
    transactions.forEach { tx ->
      addTransaction(
        tx.copy(
          id = "",
          userId = GUEST_UID,
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
      upsertSavingsGoal(goal.copy(id = "", userId = GUEST_UID))
    }
    val partyIdMap = mutableMapOf<String, String>()
    parties.forEach { party ->
      val newId = upsertParty(party.copy(id = "", userId = GUEST_UID))
      if (party.id.isNotBlank()) {
        partyIdMap[party.id] = newId
      }
    }
    partyEntries.forEach { entry ->
      val mappedPartyId = partyIdMap[entry.partyId] ?: entry.partyId
      if (mappedPartyId.isNotBlank()) {
        addPartyEntry(
          entry.copy(
            id = "",
            userId = GUEST_UID,
            partyId = mappedPartyId,
            clientRequestId = entry.clientRequestId ?: entry.id.takeIf { it.isNotBlank() },
          )
        )
      }
    }
  }

  suspend fun exportPayloadOnce(): BackupPayload {
    ensureLoaded()
    return mutex.withLock {
      BackupPayload(
        transactions = transactionsFlow.value,
        categories = categoriesFlow.value,
        budgets = budgetsFlow.value,
        goals = savingsGoalsFlow.value,
        parties = partiesFlow.value,
        partyEntries = partyEntriesFlow.value,
      )
    }
  }

  suspend fun clearAllData() {
    ensureLoaded()
    mutex.withLock {
      transactionsFlow.value = emptyList()
      categoriesFlow.value = emptyList()
      budgetsFlow.value = emptyList()
      savingsGoalsFlow.value = emptyList()
      partiesFlow.value = emptyList()
      partyEntriesFlow.value = emptyList()
      withContext(Dispatchers.IO) {
        if (storageFile.exists()) {
          storageFile.delete()
        }
      }
    }
  }

  private companion object {
    const val FILE_NAME = "guest_hisab_data.json"
    const val GUEST_UID = "guest_local"
  }
}
