package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.HisabUiState
import com.example.ui.components.AccountSwitcherPill
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormat
import com.example.util.DateFormats

enum class DateFilterOption(val labelBn: String) {
  ALL("সব তারিখ"),
  TODAY("আজ"),
  LAST_7_DAYS("গত ৭ দিন"),
  THIS_MONTH("এই মাস"),
  SPECIFIC_DATE("নির্দিষ্ট তারিখ"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
  uiState: HisabUiState,
  onSwitchAccount: (AccountType) -> Unit,
  onEditTransaction: (TransactionRecord) -> Unit,
  onDeleteTransaction: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
  var dateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
  var specificDateKey by remember { mutableStateOf<String?>(null) }
  var categoryFilter by remember { mutableStateOf<String?>(null) }
  var paymentFilter by remember { mutableStateOf<PaymentMethod?>(null) }
  var sortNewestFirst by remember { mutableStateOf(true) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showCategoryMenu by remember { mutableStateOf(false) }
  var showPaymentMenu by remember { mutableStateOf(false) }
  var pendingDeleteTx by remember { mutableStateOf<TransactionRecord?>(null) }
  var previewReceiptBase64 by remember { mutableStateOf<String?>(null) }

  val accountTxs = remember(uiState.transactions, uiState.activeAccount) {
    uiState.transactions.filter { it.accountType == uiState.activeAccount.value }
  }

  val usedCategories = remember(accountTxs) {
    accountTxs.map { it.category }.distinct().sorted()
  }

  val filteredList = remember(
    accountTxs,
    searchQuery,
    typeFilter,
    dateFilter,
    specificDateKey,
    categoryFilter,
    paymentFilter,
    sortNewestFirst,
  ) {
    val now = System.currentTimeMillis()
    val todayKey = DateFormats.formatDateKey(now)
    val sevenDaysAgo = DateFormats.daysAgoMillis(6)
    val (curY, curM, _) = DateFormats.extractYmd(now)

    val q = searchQuery.trim().lowercase()
    val filtered = accountTxs.filter { tx ->
      val matchesSearch =
        q.isEmpty() ||
          tx.category.lowercase().contains(q) ||
          tx.note.orEmpty().lowercase().contains(q) ||
          tx.paymentMethod.lowercase().contains(q) ||
          tx.amount.toString().contains(q) ||
          tx.dateKey.contains(q)

      val matchesType = typeFilter == null || tx.type == typeFilter?.value
      val matchesCat = categoryFilter == null || tx.category == categoryFilter
      val matchesPm = paymentFilter == null || tx.paymentMethod == paymentFilter?.value
      val matchesDate =
        when (dateFilter) {
          DateFilterOption.ALL -> true
          DateFilterOption.TODAY -> tx.dateKey == todayKey
          DateFilterOption.LAST_7_DAYS -> tx.dateMillis >= sevenDaysAgo
          DateFilterOption.THIS_MONTH -> tx.year == curY && tx.month == curM
          DateFilterOption.SPECIFIC_DATE -> specificDateKey == null || tx.dateKey == specificDateKey
        }
      matchesSearch && matchesType && matchesCat && matchesPm && matchesDate
    }

    if (sortNewestFirst) {
      filtered.sortedWith(
        compareByDescending<TransactionRecord> { it.dateMillis }
          .thenByDescending { it.createdAt?.seconds ?: 0L }
      )
    } else {
      filtered.sortedWith(
        compareBy<TransactionRecord> { it.dateMillis }
          .thenBy { it.createdAt?.seconds ?: 0L }
      )
    }
  }

  val totalFilteredIncome = filteredList.filter { it.isIncome }.sumOf { it.amount }
  val totalFilteredExpense = filteredList.filter { it.isExpense }.sumOf { it.amount }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "লেনদেনের ইতিহাস",
        style = MaterialTheme.typography.headlineSmall,
      )
      TextButton(
        onClick = { sortNewestFirst = !sortNewestFirst },
        modifier = Modifier.testTag("sort_toggle_button"),
      ) {
        Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (sortNewestFirst) "নতুন আগে" else "পুরাতন আগে")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    AccountSwitcherPill(
      activeAccount = uiState.activeAccount,
      onSelectAccount = onSwitchAccount,
    )

    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("ক্যাটাগরি, নোট, মাধ্যম বা টাকা খুঁজুন...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Rounded.Clear, contentDescription = "পরিষ্কার করুন")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("transaction_search_input"),
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = typeFilter == null,
        onClick = { typeFilter = null },
        label = { Text("সব") },
      )
      FilterChip(
        selected = typeFilter == TransactionType.INCOME,
        onClick = { typeFilter = if (typeFilter == TransactionType.INCOME) null else TransactionType.INCOME },
        label = { Text("আয়") },
      )
      FilterChip(
        selected = typeFilter == TransactionType.EXPENSE,
        onClick = { typeFilter = if (typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE },
        label = { Text("খরচ") },
      )

      // Date Filter Dropdown/Cycles
      DateFilterOption.entries.forEach { option ->
        if (option != DateFilterOption.SPECIFIC_DATE) {
          FilterChip(
            selected = dateFilter == option,
            onClick = { dateFilter = option },
            label = { Text(option.labelBn) },
          )
        }
      }
      FilterChip(
        selected = dateFilter == DateFilterOption.SPECIFIC_DATE,
        onClick = { showDatePicker = true },
        leadingIcon = {
          Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        label = {
          Text(specificDateKey ?: "তারিখ বাছুন")
        },
      )

      // Category Filter Menu
      Box {
        FilterChip(
          selected = categoryFilter != null,
          onClick = { showCategoryMenu = true },
          leadingIcon = {
            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          label = { Text(categoryFilter ?: "ক্যাটাগরি") },
        )
        DropdownMenu(
          expanded = showCategoryMenu,
          onDismissRequest = { showCategoryMenu = false },
        ) {
          DropdownMenuItem(
            text = { Text("সব ক্যাটাগরি") },
            onClick = {
              categoryFilter = null
              showCategoryMenu = false
            },
          )
          usedCategories.forEach { cat ->
            DropdownMenuItem(
              text = { Text(cat) },
              onClick = {
                categoryFilter = cat
                showCategoryMenu = false
              },
            )
          }
        }
      }

      // Payment Method Filter Menu
      Box {
        FilterChip(
          selected = paymentFilter != null,
          onClick = { showPaymentMenu = true },
          label = { Text(paymentFilter?.value ?: "পেমেন্ট মাধ্যম") },
        )
        DropdownMenu(
          expanded = showPaymentMenu,
          onDismissRequest = { showPaymentMenu = false },
        ) {
          DropdownMenuItem(
            text = { Text("সব মাধ্যম") },
            onClick = {
              paymentFilter = null
              showPaymentMenu = false
            },
          )
          PaymentMethod.entries.forEach { pm ->
            DropdownMenuItem(
              text = { Text(pm.labelBn) },
              onClick = {
                paymentFilter = pm
                showPaymentMenu = false
              },
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Filtered summary strip
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "মোট ${filteredList.size}টি",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "আয়: ${CurrencyFormat.formatCompactBdt(totalFilteredIncome)}  |  খরচ: ${CurrencyFormat.formatCompactBdt(totalFilteredExpense)}",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (filteredList.isEmpty()) {
      EmptyStateCard(
        title = "কোনো লেনদেন পাওয়া যায়নি",
        subtitle = "ফিল্টার পরিবর্তন করুন অথবা নতুন লেনদেন যোগ করুন।",
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredList, key = { it.id }) { tx ->
          TransactionHistoryCard(
            tx = tx,
            onEdit = { onEditTransaction(tx) },
            onDelete = { pendingDeleteTx = tx },
            onViewReceipt = { tx.receiptBase64?.let { previewReceiptBase64 = it } },
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val state = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            state.selectedDateMillis?.let {
              specificDateKey = DateFormats.formatDateKey(it)
              dateFilter = DateFilterOption.SPECIFIC_DATE
            }
            showDatePicker = false
          }
        ) {
          Text("বাছাই করুন")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            specificDateKey = null
            dateFilter = DateFilterOption.ALL
            showDatePicker = false
          }
        ) {
          Text("সব তারিখ")
        }
      },
    ) {
      DatePicker(state = state)
    }
  }

  pendingDeleteTx?.let { tx ->
    ConfirmDeleteDialog(
      title = "লেনদেন মুছবেন?",
      message = "${tx.category} (${CurrencyFormat.formatBdt(tx.amount)}, ${tx.dateKey}) স্থায়ীভাবে মুছে ফেলা হবে।",
      onConfirm = {
        onDeleteTransaction(tx.id)
        pendingDeleteTx = null
      },
      onDismiss = { pendingDeleteTx = null },
    )
  }

  previewReceiptBase64?.let { b64 ->
    ReceiptPreviewDialog(
      receiptBase64 = b64,
      onDismiss = { previewReceiptBase64 = null },
    )
  }
}

@Composable
private fun TransactionHistoryCard(
  tx: TransactionRecord,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onViewReceipt: () -> Unit,
) {
  val accentColor = if (tx.isIncome) IncomeGreen else ExpenseRed
  val sign = if (tx.isIncome) "+" else "-"
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onEdit),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = accentColor.copy(alpha = 0.14f),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (tx.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
              contentDescription = null,
              tint = accentColor,
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tx.category,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${tx.dateKey} • ${tx.paymentMethod}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "$sign${CurrencyFormat.formatBdt(tx.amount)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
      }

      if (!tx.note.isNullOrBlank() || !tx.receiptBase64.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = tx.note.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (!tx.receiptBase64.isNullOrBlank()) {
            TextButton(onClick = onViewReceipt) {
              Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("রসিদ")
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onEdit) {
          Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("সম্পাদনা")
        }
        TextButton(onClick = onDelete) {
          Icon(
            Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("মুছুন", color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}
