package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.HisabRepository
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetStatus
import com.example.domain.DashboardSummary
import com.example.domain.HisabAnalytics
import com.example.domain.MonthlyReportData
import com.example.util.BackupPayload
import com.example.util.DateFormats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HisabUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val activeAccount: AccountType = AccountType.PERSONAL,
  val transactions: List<TransactionRecord> = emptyList(),
  val customCategories: List<CategoryRecord> = emptyList(),
  val budgets: List<BudgetRecord> = emptyList(),
  val savingsGoals: List<SavingsGoalRecord> = emptyList(),
  val dashboard: DashboardSummary = DashboardSummary(),
  val selectedReportYear: Int = DateFormats.extractYmd(System.currentTimeMillis()).first,
  val selectedReportMonth: Int = DateFormats.extractYmd(System.currentTimeMillis()).second,
  val monthlyReport: MonthlyReportData = HisabAnalytics.computeMonthlyReport(
    emptyList(),
    AccountType.PERSONAL,
    DateFormats.extractYmd(System.currentTimeMillis()).first,
    DateFormats.extractYmd(System.currentTimeMillis()).second,
  ),
  val budgetStatuses: List<BudgetStatus> = emptyList(),
  val triggeredBudgetAlerts: List<BudgetStatus> = emptyList(),
  val errorMessage: String? = null,
  val snackbarMessage: String? = null,
)

class HisabViewModel(
  private val repository: HisabRepository,
  private val preferences: AppPreferences,
  private val currentUserId: String,
  userDisplayName: String?,
  userEmail: String?,
) : ViewModel() {

  private val nowInit = DateFormats.extractYmd(System.currentTimeMillis())
  private val reportMonthYear = MutableStateFlow(nowInit.first to nowInit.second)
  private val isSubmittingFlow = MutableStateFlow(false)
  private val errorFlow = MutableStateFlow<String?>(null)
  private val snackbarFlow = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      try {
        repository.ensureUserProfile(userDisplayName, userEmail)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Profile init warning: ${e.message}")
      }
    }
  }

  private val transactionsFlow =
    repository
      .observeTransactions(currentUserId)
      .catch { error ->
        emitErrorFromFlow(error, "লেনদেনের তথ্য লোড করতে সমস্যা হয়েছে")
        emit(emptyList())
      }

  private val categoriesFlow =
    repository
      .observeCustomCategories(currentUserId)
      .catch { error ->
        emitErrorFromFlow(error, "ক্যাটাগরি লোড করতে সমস্যা হয়েছে")
        emit(emptyList())
      }

  private val budgetsFlow =
    repository
      .observeBudgets(currentUserId)
      .catch { error ->
        emitErrorFromFlow(error, "বাজেট লোড করতে সমস্যা হয়েছে")
        emit(emptyList())
      }

  private val savingsFlow =
    repository
      .observeSavingsGoals(currentUserId)
      .catch { error ->
        emitErrorFromFlow(error, "সঞ্চয় লক্ষ্য লোড করতে সমস্যা হয়েছে")
        emit(emptyList())
      }

  private data class CoreStreams(
    val account: AccountType,
    val transactions: List<TransactionRecord>,
    val categories: List<CategoryRecord>,
    val budgets: List<BudgetRecord>,
    val goals: List<SavingsGoalRecord>,
  )

  private val coreStreamsFlow =
    combine(
      preferences.activeAccount,
      transactionsFlow,
      categoriesFlow,
      budgetsFlow,
      savingsFlow,
    ) { account, txs, cats, budgets, goals ->
      CoreStreams(account, txs, cats, budgets, goals)
    }

  val uiState: StateFlow<HisabUiState> =
    combine(
      coreStreamsFlow,
      reportMonthYear,
      isSubmittingFlow,
      errorFlow,
      snackbarFlow,
    ) { core, (repYear, repMonth), submitting, errMsg, snackMsg ->
      val now = System.currentTimeMillis()
      val (curYear, curMonth, _) = DateFormats.extractYmd(now)
      val dashboard = HisabAnalytics.computeDashboard(core.transactions, core.account, now)
      val report =
        HisabAnalytics.computeMonthlyReport(core.transactions, core.account, repYear, repMonth, now)
      val budgetStatuses =
        HisabAnalytics.computeBudgetStatuses(
          core.budgets,
          core.transactions,
          core.account,
          curYear,
          curMonth,
        )
      val alerts = budgetStatuses.filter { it.alertLevel != BudgetAlertLevel.NORMAL }

      HisabUiState(
        isLoading = false,
        isSubmitting = submitting,
        activeAccount = core.account,
        transactions = core.transactions,
        customCategories = core.categories,
        budgets = core.budgets,
        savingsGoals = core.goals,
        dashboard = dashboard,
        selectedReportYear = repYear,
        selectedReportMonth = repMonth,
        monthlyReport = report,
        budgetStatuses = budgetStatuses,
        triggeredBudgetAlerts = alerts,
        errorMessage = errMsg,
        snackbarMessage = snackMsg,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = HisabUiState(isLoading = true),
    )

  private fun emitErrorFromFlow(error: Throwable, fallback: String) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Stream error", rootError)
    errorFlow.value = rootError.localizedMessage ?: fallback
  }

  fun switchAccount(account: AccountType) {
    viewModelScope.launch {
      preferences.setActiveAccount(account)
      try {
        repository.updateActiveAccount(account)
      } catch (_: Exception) {
        // DataStore already updated locally; Firestore syncs when online
      }
    }
  }

  fun selectReportMonthYear(year: Int, month: Int) {
    reportMonthYear.value = year to month
  }

  fun clearMessages() {
    errorFlow.value = null
    snackbarFlow.value = null
  }

  fun showMessage(message: String) {
    snackbarFlow.value = message
  }

  fun saveTransaction(
    editingId: String?,
    accountType: AccountType,
    type: TransactionType,
    amount: Double,
    category: String,
    paymentMethod: PaymentMethod,
    dateMillis: Long,
    note: String?,
    receiptBase64: String?,
    clientRequestId: String,
    onSuccess: () -> Unit,
  ) {
    if (isSubmittingFlow.value) return
    viewModelScope.launch {
      isSubmittingFlow.value = true
      try {
        val record =
          TransactionRecord(
            id = editingId.orEmpty(),
            userId = currentUserId,
            accountType = accountType.value,
            type = type.value,
            amount = amount,
            category = category.trim(),
            paymentMethod = paymentMethod.value,
            dateMillis = dateMillis,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            receiptBase64 = receiptBase64,
            clientRequestId = clientRequestId,
          )
        if (editingId.isNullOrBlank()) {
          repository.addTransaction(record)
          snackbarFlow.value = "লেনদেন সফলভাবে সংরক্ষিত হয়েছে"
        } else {
          repository.updateTransaction(record)
          snackbarFlow.value = "লেনদেন আপডেট করা হয়েছে"
        }
        onSuccess()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "লেনদেন সংরক্ষণ করা যায়নি"
      } finally {
        isSubmittingFlow.value = false
      }
    }
  }

  fun deleteTransaction(id: String) {
    viewModelScope.launch {
      try {
        repository.deleteTransaction(id)
        snackbarFlow.value = "লেনদেন মুছে ফেলা হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "লেনদেন মুছে ফেলা যায়নি"
      }
    }
  }

  fun addCustomCategory(name: String, type: TransactionType, onSuccess: () -> Unit = {}) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      try {
        repository.addCustomCategory(clean, type.value)
        snackbarFlow.value = "নতুন ক্যাটাগরি যোগ হয়েছে"
        onSuccess()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "ক্যাটাগরি যোগ করা যায়নি"
      }
    }
  }

  fun deleteCustomCategory(categoryId: String) {
    viewModelScope.launch {
      try {
        repository.deleteCustomCategory(categoryId)
        snackbarFlow.value = "ক্যাটাগরি মুছে ফেলা হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "ক্যাটাগরি মুছে ফেলা যায়নি"
      }
    }
  }

  fun saveBudget(
    existingId: String?,
    accountType: AccountType,
    category: String,
    limitAmount: Double,
    year: Int,
    month: Int,
    onSuccess: () -> Unit,
  ) {
    if (isSubmittingFlow.value) return
    viewModelScope.launch {
      isSubmittingFlow.value = true
      try {
        repository.upsertBudget(
          existingId = existingId,
          accountType = accountType.value,
          year = year,
          month = month,
          category = category,
          limitAmount = limitAmount,
        )
        snackbarFlow.value = "বাজেট সংরক্ষিত হয়েছে"
        onSuccess()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "বাজেট সংরক্ষণ করা যায়নি"
      } finally {
        isSubmittingFlow.value = false
      }
    }
  }

  fun deleteBudget(budgetId: String) {
    viewModelScope.launch {
      try {
        repository.deleteBudget(budgetId)
        snackbarFlow.value = "বাজেট মুছে ফেলা হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "বাজেট মুছে ফেলা যায়নি"
      }
    }
  }

  fun saveSavingsGoal(
    editingId: String?,
    accountType: AccountType,
    name: String,
    targetAmount: Double,
    savedAmount: Double,
    targetDateMillis: Long,
    onSuccess: () -> Unit,
  ) {
    if (isSubmittingFlow.value) return
    viewModelScope.launch {
      isSubmittingFlow.value = true
      try {
        repository.upsertSavingsGoal(
          SavingsGoalRecord(
            id = editingId.orEmpty(),
            userId = currentUserId,
            accountType = accountType.value,
            name = name.trim(),
            targetAmount = targetAmount,
            savedAmount = savedAmount,
            targetDateMillis = targetDateMillis,
          )
        )
        snackbarFlow.value = "সঞ্চয় লক্ষ্য সংরক্ষিত হয়েছে"
        onSuccess()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "সঞ্চয় লক্ষ্য সংরক্ষণ করা যায়নি"
      } finally {
        isSubmittingFlow.value = false
      }
    }
  }

  fun addSavingsContribution(goal: SavingsGoalRecord, addAmount: Double) {
    if (addAmount <= 0.0) return
    viewModelScope.launch {
      try {
        repository.upsertSavingsGoal(
          goal.copy(savedAmount = goal.savedAmount + addAmount)
        )
        snackbarFlow.value = "সঞ্চয়ে টাকা যোগ হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "সঞ্চয় আপডেট করা যায়নি"
      }
    }
  }

  fun deleteSavingsGoal(goalId: String) {
    viewModelScope.launch {
      try {
        repository.deleteSavingsGoal(goalId)
        snackbarFlow.value = "সঞ্চয় লক্ষ্য মুছে ফেলা হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "সঞ্চয় লক্ষ্য মুছে ফেলা যায়নি"
      }
    }
  }

  fun restoreBackup(payload: BackupPayload) {
    if (isSubmittingFlow.value) return
    viewModelScope.launch {
      isSubmittingFlow.value = true
      try {
        repository.restoreBackup(
          transactions = payload.transactions,
          categories = payload.categories,
          budgets = payload.budgets,
          goals = payload.goals,
        )
        snackbarFlow.value = "ব্যাকআপ সফলভাবে রিস্টোর হয়েছে"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        errorFlow.value = e.localizedMessage ?: "ব্যাকআপ রিস্টোর ব্যর্থ হয়েছে"
      } finally {
        isSubmittingFlow.value = false
      }
    }
  }

  private companion object {
    const val TAG = "HisabVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
