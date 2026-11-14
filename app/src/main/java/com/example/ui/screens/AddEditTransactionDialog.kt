package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.CategoryRecord
import com.example.data.model.DefaultCategories
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.DateFormats
import com.example.util.ReceiptImageHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
  editingTransaction: TransactionRecord?,
  defaultAccount: AccountType,
  customCategories: List<CategoryRecord>,
  isSubmitting: Boolean,
  onSave: (
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
  ) -> Unit,
  onAddCustomCategory: (String, TransactionType) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedAccount by remember {
    mutableStateOf(
      editingTransaction?.let { AccountType.fromValue(it.accountType) } ?: defaultAccount
    )
  }
  var txType by remember {
    mutableStateOf(
      editingTransaction?.let { TransactionType.fromValue(it.type) } ?: TransactionType.EXPENSE
    )
  }
  var amountText by remember {
    mutableStateOf(
      editingTransaction?.amount?.let {
        if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
      } ?: ""
    )
  }

  val availableCategories = remember(txType, customCategories) {
    val base = if (txType == TransactionType.EXPENSE) DefaultCategories.EXPENSE else DefaultCategories.INCOME
    val custom = customCategories.filter { it.type == txType.value }.map { it.name }
    (base + custom).distinct()
  }

  var selectedCategory by remember(txType) {
    mutableStateOf(
      editingTransaction?.category?.takeIf { it in availableCategories }
        ?: availableCategories.firstOrNull().orEmpty()
    )
  }
  var paymentMethod by remember {
    mutableStateOf(
      editingTransaction?.let { PaymentMethod.fromValue(it.paymentMethod) } ?: PaymentMethod.CASH
    )
  }
  var selectedDateMillis by remember {
    mutableLongStateOf(editingTransaction?.dateMillis ?: System.currentTimeMillis())
  }
  var noteText by remember { mutableStateOf(editingTransaction?.note.orEmpty()) }
  var receiptBase64 by remember { mutableStateOf(editingTransaction?.receiptBase64) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }
  var showCustomCatDialog by remember { mutableStateOf(false) }
  var newCatName by remember { mutableStateOf("") }

  // Idempotent clientRequestId created once per sheet session to prevent duplicates
  val clientRequestId = remember {
    editingTransaction?.clientRequestId ?: "tx_${UUID.randomUUID().toString().replace("-", "")}"
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        val encoded = ReceiptImageHelper.uriToCompressedBase64(context, uri)
        if (encoded != null) {
          receiptBase64 = encoded
        }
      }
    },
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (editingTransaction == null) "নতুন লেনদেন যোগ করুন" else "লেনদেন সম্পাদনা",
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
        }
      }

      // Expense / Income Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TransactionType.entries.forEach { typeOption ->
          val isSelected = txType == typeOption
          val activeBg = if (typeOption == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable { txType = typeOption }
              .testTag("tx_type_${typeOption.value}"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 12.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = typeOption.labelBn,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Account Switcher (Personal / Business)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AccountType.entries.forEach { acc ->
          FilterChip(
            selected = selectedAccount == acc,
            onClick = { selectedAccount = acc },
            label = { Text(acc.labelBn) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Amount Input (BDT ৳)
      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          val filtered = input.filter { it.isDigit() || it == '.' }
          if (filtered.count { it == '.' } <= 1) {
            amountText = filtered
            amountError = null
          }
        },
        label = { Text("পরিমাণ (৳)") },
        prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = amountError != null,
        supportingText = amountError?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tx_amount_input"),
      )

      // Date Picker Row
      OutlinedButton(
        onClick = { showDatePicker = true },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("tx_date_button"),
      ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("তারিখ: ${DateFormats.formatDateKey(selectedDateMillis)}")
      }

      // Categories Grid + Custom Category Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("ক্যাটাগরি নির্বাচন করুন", style = MaterialTheme.typography.titleSmall)
        TextButton(
          onClick = { showCustomCatDialog = true },
          modifier = Modifier.testTag("add_custom_category_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("কাস্টম ক্যাটাগরি")
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        availableCategories.forEach { cat ->
          val selected = selectedCategory == cat
          FilterChip(
            selected = selected,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          )
        }
      }

      // Payment Methods
      Text("পেমেন্ট মাধ্যম", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PaymentMethod.entries.forEach { pm ->
          FilterChip(
            selected = paymentMethod == pm,
            onClick = { paymentMethod = pm },
            label = { Text(pm.value) },
          )
        }
      }

      // Note / Description
      OutlinedTextField(
        value = noteText,
        onValueChange = { if (it.length <= 300) noteText = it },
        label = { Text("বিবরণ / নোট (ঐচ্ছিক)") },
        maxLines = 3,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tx_note_input"),
      )

      // Optional Receipt Image (Zero-permission Photo Picker)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("tx_pick_receipt_button"),
        ) {
          Icon(Icons.Rounded.Image, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (receiptBase64 == null) "রসিদের ছবি যোগ করুন" else "রসিদের ছবি পরিবর্তন")
        }
        if (receiptBase64 != null) {
          IconButton(onClick = { receiptBase64 = null }) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = "রসিদ সরান",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      val previewBitmap = remember(receiptBase64) {
        ReceiptImageHelper.decodeBase64ToImageBitmap(receiptBase64)
      }
      if (previewBitmap != null) {
        Image(
          bitmap = previewBitmap,
          contentDescription = "নির্বাচিত রসিদ",
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentScale = ContentScale.Crop,
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Button(
        onClick = {
          val parsed = amountText.toDoubleOrNull()
          if (parsed == null || parsed <= 0.0) {
            amountError = "সঠিক পরিমাণ লিখুন (০ এর বেশি)"
            return@Button
          }
          if (selectedCategory.isBlank()) {
            amountError = "একটি ক্যাটাগরি নির্বাচন করুন"
            return@Button
          }
          onSave(
            editingTransaction?.id,
            selectedAccount,
            txType,
            parsed,
            selectedCategory,
            paymentMethod,
            selectedDateMillis,
            noteText,
            receiptBase64,
            clientRequestId,
          )
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("save_transaction_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text(
            text = if (editingTransaction == null) "লেনদেন সংরক্ষণ করুন" else "আপডেট করুন",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
            showDatePicker = false
          }
        ) {
          Text("ঠিক আছে")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("বাতিল")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showCustomCatDialog) {
    AlertDialog(
      onDismissRequest = { showCustomCatDialog = false },
      title = { Text("নতুন ক্যাটাগরি (${txType.labelBn})") },
      text = {
        OutlinedTextField(
          value = newCatName,
          onValueChange = { if (it.length <= 40) newCatName = it },
          label = { Text("ক্যাটাগরির নাম") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newCatName.trim()
            if (clean.isNotEmpty()) {
              onAddCustomCategory(clean, txType)
              selectedCategory = clean
              newCatName = ""
              showCustomCatDialog = false
            }
          }
        ) {
          Text("যোগ করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomCatDialog = false }) {
          Text("বাতিল")
        }
      },
    )
  }
}
