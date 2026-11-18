package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.data.model.PaymentMethod
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.DateFormats
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPartySheet(
  editingParty: PartyRecord?,
  defaultPartyType: PartyType = PartyType.CUSTOMER,
  isSubmitting: Boolean,
  onSave: (
    editingId: String?,
    partyType: PartyType,
    name: String,
    phone: String?,
    address: String?,
    note: String?,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var partyType by remember {
    mutableStateOf(editingParty?.role ?: defaultPartyType)
  }
  var name by remember { mutableStateOf(editingParty?.name.orEmpty()) }
  var phone by remember { mutableStateOf(editingParty?.phone.orEmpty()) }
  var address by remember { mutableStateOf(editingParty?.address.orEmpty()) }
  var note by remember { mutableStateOf(editingParty?.note.orEmpty()) }
  var nameError by remember { mutableStateOf<String?>(null) }

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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (editingParty == null) "নতুন কাস্টমার / সাপ্লায়ার" else "তথ্য সম্পাদনা",
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
        }
      }

      // Party Type Switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PartyType.entries.forEach { roleOption ->
          val selected = partyType == roleOption
          val activeBg =
            if (roleOption == PartyType.CUSTOMER) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (selected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable { partyType = roleOption }
              .testTag("party_type_${roleOption.value}"),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (roleOption == PartyType.CUSTOMER) Icons.Rounded.Person else Icons.Rounded.Storefront,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = roleOption.labelBn,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = {
          if (it.length <= 100) {
            name = it
            nameError = null
          }
        },
        label = { Text("${partyType.labelBn}-এর নাম *") },
        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        isError = nameError != null,
        supportingText = nameError?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("party_name_input"),
      )

      OutlinedTextField(
        value = phone,
        onValueChange = { if (it.length <= 30) phone = it },
        label = { Text("মোবাইল নম্বর (হোয়াটসঅ্যাপ ও কলের জন্য)") },
        placeholder = { Text("যেমন: 017XXXXXXXX") },
        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("party_phone_input"),
      )

      OutlinedTextField(
        value = address,
        onValueChange = { if (it.length <= 200) address = it },
        label = { Text("ঠিকানা / দোকানের নাম (ঐচ্ছিক)") },
        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = note,
        onValueChange = { if (it.length <= 300) note = it },
        label = { Text("নোট (ঐচ্ছিক)") },
        maxLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          val clean = name.trim()
          if (clean.isEmpty()) {
            nameError = "অনুগ্রহ করে নাম লিখুন"
            return@Button
          }
          onSave(
            editingParty?.id,
            partyType,
            clean,
            phone.trim().takeIf { it.isNotEmpty() },
            address.trim().takeIf { it.isNotEmpty() },
            note.trim().takeIf { it.isNotEmpty() },
          )
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("save_party_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text(
            text = if (editingParty == null) "${partyType.labelBn} যোগ করুন" else "আপডেট করুন",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPartyEntrySheet(
  parties: List<PartyRecord>,
  initialPartyId: String?,
  initialEntryType: PartyEntryType = PartyEntryType.GIVEN,
  editingEntry: PartyEntryRecord?,
  isSubmitting: Boolean,
  onQuickAddParty: () -> Unit,
  onSave: (
    editingId: String?,
    partyId: String,
    entryType: PartyEntryType,
    amount: Double,
    paymentMethod: PaymentMethod,
    dateMillis: Long,
    note: String?,
    clientRequestId: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedPartyId by remember(parties) {
    mutableStateOf(
      editingEntry?.partyId?.takeIf { id -> parties.any { it.id == id } }
        ?: initialPartyId?.takeIf { id -> parties.any { it.id == id } }
        ?: parties.firstOrNull()?.id.orEmpty()
    )
  }
  val selectedParty = parties.firstOrNull { it.id == selectedPartyId }
  val partyRole = selectedParty?.role ?: PartyType.CUSTOMER

  var entryType by remember {
    mutableStateOf(editingEntry?.kind ?: initialEntryType)
  }
  var amountText by remember {
    mutableStateOf(
      editingEntry?.amount?.let {
        if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
      }.orEmpty()
    )
  }
  var paymentMethod by remember {
    mutableStateOf(
      editingEntry?.let { PaymentMethod.fromValue(it.paymentMethod) } ?: PaymentMethod.CASH
    )
  }
  var selectedDateMillis by remember {
    mutableLongStateOf(editingEntry?.dateMillis ?: System.currentTimeMillis())
  }
  var noteText by remember { mutableStateOf(editingEntry?.note.orEmpty()) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  val clientRequestId = remember {
    editingEntry?.clientRequestId ?: "pe_${UUID.randomUUID().toString().replace("-", "")}"
  }

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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (editingEntry == null) "ট্যালি খাতায় এন্ট্রি" else "এন্ট্রি সম্পাদনা",
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
        }
      }

      // Party Picker (when multiple or no fixed party)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("কাস্টমার / সাপ্লায়ার", style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = onQuickAddParty) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("নতুন যোগ করুন")
        }
      }

      if (parties.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "এখনো কোনো কাস্টমার বা সাপ্লায়ার যোগ করা হয়নি। উপরের 'নতুন যোগ করুন' বোতামে ট্যাপ করুন।",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          parties.forEach { party ->
            FilterChip(
              selected = selectedPartyId == party.id,
              onClick = { selectedPartyId = party.id },
              label = { Text("${party.name} (${party.role.labelBn})") },
            )
          }
        }
      }

      // Given / Received Toggle with contextual Bengali labels
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PartyEntryType.entries.forEach { option ->
          val isSelected = entryType == option
          // For Customer: GIVEN = Baki Given (rose/red), RECEIVED = Cash Received (green)
          // For Supplier: GIVEN = Payment Given (green), RECEIVED = Baki Purchase (rose/red)
          val isCreditIncrease =
            (partyRole == PartyType.CUSTOMER && option == PartyEntryType.GIVEN) ||
              (partyRole == PartyType.SUPPLIER && option == PartyEntryType.RECEIVED)
          val activeBg = if (isCreditIncrease) ExpenseRed else IncomeGreen
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable { entryType = option }
              .testTag("party_entry_type_${option.value}"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = option.contextLabel(partyRole),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      // Amount Input
      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          val filtered = input.filter { it.isDigit() || it == '.' }
          if (filtered.count { it == '.' } <= 1) {
            amountText = filtered
            amountError = null
          }
        },
        label = { Text("টাকার পরিমাণ (৳)") },
        prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = amountError != null,
        supportingText = amountError?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("party_entry_amount_input"),
      )

      // Date Picker Row
      OutlinedButton(
        onClick = { showDatePicker = true },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
      ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("তারিখ: ${DateFormats.formatDateKey(selectedDateMillis)}")
      }

      // Payment Methods
      Text("লেনদেনের মাধ্যম", style = MaterialTheme.typography.titleSmall)
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

      OutlinedTextField(
        value = noteText,
        onValueChange = { if (it.length <= 300) noteText = it },
        label = { Text("বিবরণ / পণ্যের নাম (ঐচ্ছিক)") },
        maxLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("party_entry_note_input"),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          if (selectedPartyId.isBlank()) {
            amountError = "একজন কাস্টমার বা সাপ্লায়ার নির্বাচন করুন"
            return@Button
          }
          val parsed = amountText.toDoubleOrNull()
          if (parsed == null || parsed <= 0.0) {
            amountError = "সঠিক পরিমাণ লিখুন (০ এর বেশি)"
            return@Button
          }
          onSave(
            editingEntry?.id,
            selectedPartyId,
            entryType,
            parsed,
            paymentMethod,
            selectedDateMillis,
            noteText,
            clientRequestId,
          )
        },
        enabled = !isSubmitting && selectedPartyId.isNotBlank(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("save_party_entry_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text(
            text = if (editingEntry == null) "খাতায় সংরক্ষণ করুন" else "আপডেট করুন",
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
}
