package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemePreference
import com.example.data.model.AccountType
import com.example.data.model.CategoryRecord
import com.example.data.model.TransactionType
import com.example.ui.HisabUiState
import com.example.ui.components.AccountSwitcherPill
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.neumorphicCircle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  uiState: HisabUiState,
  userName: String?,
  userEmail: String?,
  themePreference: ThemePreference,
  onChangeTheme: (ThemePreference) -> Unit,
  onSwitchAccount: (AccountType) -> Unit,
  onAddCustomCategory: (String, TransactionType) -> Unit,
  onDeleteCustomCategory: (String) -> Unit,
  onExportPdf: () -> Unit,
  onExportExcel: () -> Unit,
  onBackupData: () -> Unit,
  onRestoreData: () -> Unit,
  onSignOut: () -> Unit,
  isGuestMode: Boolean = false,
  isLinkingGoogle: Boolean = false,
  onLinkGoogleAccount: () -> Unit = {},
  appLockEnabled: Boolean = false,
  onSetAppLock: (enabled: Boolean, pin: String?) -> Unit = { _, _ -> },
  onVerifyPin: suspend (String) -> Boolean = { true },
  modifier: Modifier = Modifier,
) {
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  var showAddCatDialog by remember { mutableStateOf(false) }
  var pendingDeleteCat by remember { mutableStateOf<CategoryRecord?>(null) }
  var showSignOutConfirm by remember { mutableStateOf(false) }

  // App Lock dialog states
  var showSetPinDialog by remember { mutableStateOf(false) }
  var showVerifyPinToDisableDialog by remember { mutableStateOf(false) }
  var showVerifyPinToChangeDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "সেটিংস ও ব্যাকআপ",
        style = MaterialTheme.typography.headlineSmall,
      )
    }

    // User Account & Cloud Sync Card
    item {
      NeumorphicCard(
        cornerRadius = 22.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .neumorphicCircle(
                  shadowElevation = 4.dp,
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = (userName?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = userName?.takeIf { it.isNotBlank() }
                  ?: if (isGuestMode) "অফলাইন গেস্ট" else "ব্যবহারকারী",
                style = MaterialTheme.typography.titleMedium,
              )
              if (!userEmail.isNullOrBlank()) {
                Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isGuestMode) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                  contentDescription = null,
                  tint = if (isGuestMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isGuestMode) "অফলাইন মোড — শুধু এই ফোনে সংরক্ষিত" else "Firebase ক্লাউড সিঙ্ক সক্রিয়",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isGuestMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          if (isGuestMode) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onLinkGoogleAccount,
              enabled = !isLinkingGoogle,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("link_google_account_button"),
            ) {
              if (isLinkingGoogle) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Login,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google অ্যাকাউন্ট যুক্ত ও সিঙ্ক করুন")
              }
            }
          }
        }
      }
    }

    // Security & App Lock Card
    item {
      NeumorphicCard(
        cornerRadius = 20.dp,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .neumorphicCircle(
                    shadowElevation = 3.dp,
                    containerColor = if (appLockEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (appLockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                  contentDescription = null,
                  tint = if (appLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "অ্যাপ লক (৪-সংখ্যার পিন)",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = if (appLockEnabled) "লক সক্রিয় — অ্যাপ খোলার সময় পিন চাইবে" else "নিষ্ক্রিয় — কোনো পিন ছাড়াই অ্যাপ খুলবে",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = appLockEnabled,
              onCheckedChange = { isChecked ->
                if (isChecked) {
                  showSetPinDialog = true
                } else {
                  showVerifyPinToDisableDialog = true
                }
              },
              modifier = Modifier.testTag("app_lock_switch"),
            )
          }

          if (appLockEnabled) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              OutlinedButton(
                onClick = { showVerifyPinToChangeDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("change_pin_button"),
              ) {
                Icon(Icons.Rounded.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("পিন পরিবর্তন করুন", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }

    // Account Switcher
    item {
      NeumorphicCard(
        cornerRadius = 20.dp,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("সক্রিয় হিসাব", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(10.dp))
          AccountSwitcherPill(
            activeAccount = uiState.activeAccount,
            onSelectAccount = onSwitchAccount,
          )
        }
      }
    }

    // Theme Mode (Light / Dark / System)
    item {
      NeumorphicCard(
        cornerRadius = 20.dp,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("অ্যাপ থিম", style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThemePreference.entries.forEach { pref ->
              FilterChip(
                selected = themePreference == pref,
                onClick = { onChangeTheme(pref) },
                label = { Text(pref.labelBn) },
                modifier = Modifier.testTag("theme_chip_${pref.value}"),
              )
            }
          }
        }
      }
    }

    // Custom Categories Management
    item {
      NeumorphicCard(
        cornerRadius = 20.dp,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("কাস্টম ক্যাটাগরি", style = MaterialTheme.typography.titleMedium)
            TextButton(
              onClick = { showAddCatDialog = true },
              modifier = Modifier.testTag("settings_add_category_button"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("নতুন যোগ")
            }
          }
          if (uiState.customCategories.isEmpty()) {
            Text(
              text = "এখনো কোনো কাস্টম ক্যাটাগরি তৈরি করা হয়নি।",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              uiState.customCategories.forEach { cat ->
                InputChip(
                  selected = false,
                  onClick = {},
                  label = {
                    val tag = if (cat.type == TransactionType.INCOME.value) "আয়" else "খরচ"
                    Text("${cat.name} ($tag)")
                  },
                  trailingIcon = {
                    IconButton(
                      onClick = { pendingDeleteCat = cat },
                      modifier = Modifier.size(20.dp),
                    ) {
                      Icon(Icons.Rounded.Close, contentDescription = "মুছুন", modifier = Modifier.size(14.dp))
                    }
                  },
                )
              }
            }
          }
        }
      }
    }

    // Export Reports & Backup / Restore
    item {
      NeumorphicCard(
        cornerRadius = 20.dp,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("রিপোর্ট এক্সপোর্ট ও ব্যাকআপ", style = MaterialTheme.typography.titleMedium)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onExportPdf,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("settings_export_pdf_button"),
            ) {
              Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("PDF রিপোর্ট")
            }
            OutlinedButton(
              onClick = onExportExcel,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("settings_export_excel_button"),
            ) {
              Icon(Icons.Rounded.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Excel (.xlsx)")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onBackupData,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("backup_data_button"),
            ) {
              Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("ব্যাকআপ নিন")
            }
            OutlinedButton(
              onClick = onRestoreData,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("restore_data_button"),
            ) {
              Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("রিস্টোর করুন")
            }
          }
        }
      }
    }

    // Sign Out / Return to Login
    item {
      OutlinedButton(
        onClick = { showSignOutConfirm = true },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isGuestMode) "লগইন স্ক্রিনে ফিরে যান" else "সাইন আউট করুন",
          fontWeight = FontWeight.Bold,
        )
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }

  if (showAddCatDialog) {
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf(TransactionType.EXPENSE) }
    AlertDialog(
      onDismissRequest = { showAddCatDialog = false },
      title = { Text("নতুন কাস্টম ক্যাটাগরি") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionType.entries.forEach { t ->
              FilterChip(
                selected = catType == t,
                onClick = { catType = t },
                label = { Text(t.labelBn) },
              )
            }
          }
          OutlinedTextField(
            value = catName,
            onValueChange = { if (it.length <= 40) catName = it },
            label = { Text("ক্যাটাগরির নাম") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          if (catName.isNotBlank()) {
            onAddCustomCategory(catName.trim(), catType)
            showAddCatDialog = false
          }
        }) { Text("যোগ করুন") }
      },
      dismissButton = {
        TextButton(onClick = { showAddCatDialog = false }) { Text("বাতিল") }
      },
    )
  }

  pendingDeleteCat?.let { cat ->
    ConfirmDeleteDialog(
      title = "ক্যাটাগরি মুছবেন?",
      message = "\"${cat.name}\" ক্যাটাগরিটি মুছে ফেলা হবে।",
      onConfirm = {
        onDeleteCustomCategory(cat.id)
        pendingDeleteCat = null
      },
      onDismiss = { pendingDeleteCat = null },
    )
  }

  if (showSignOutConfirm) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirm = false },
      title = {
        Text(if (isGuestMode) "লগইন স্ক্রিনে ফিরে যাবেন?" else "সাইন আউট করবেন?")
      },
      text = {
        Text(
          if (isGuestMode) {
            "লগইন স্ক্রিনে ফিরে গেলেও আপনার অফলাইন হিসাব এই ফোনে সংরক্ষিত থাকবে। পরে আবার অফলাইন মোডে ঢুকলে বা Google অ্যাকাউন্ট যুক্ত করলে সব তথ্য পাবেন।"
          } else {
            "আপনার সব হিসাব Firebase ক্লাউডে নিরাপদে সংরক্ষিত আছে। পুনরায় লগইন করলেই সব তথ্য ফিরে পাবেন।"
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirm = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(if (isGuestMode) "ফিরে যান" else "সাইন আউট")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirm = false }) { Text("বাতিল") }
      },
    )
  }

  // --- App Lock: Set New PIN Dialog ---
  if (showSetPinDialog) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showSetPinDialog = false },
      title = { Text("নতুন ৪ সংখ্যার পিন নির্ধারণ করুন") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "অ্যাপ সুরক্ষিত রাখতে ৪ সংখ্যার একটি গোপন পিন সেট করুন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = newPin,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                newPin = input
                pinError = null
              }
            },
            label = { Text("নতুন ৪ সংখ্যার পিন") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_pin_input"),
          )
          OutlinedTextField(
            value = confirmPin,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                confirmPin = input
                pinError = null
              }
            },
            label = { Text("পিন পুনরায় লিখুন (কনফার্ম)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
          )
          if (pinError != null) {
            Text(
              text = pinError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newPin.length != 4) {
              pinError = "পিন অবশ্যই ঠিক ৪ সংখ্যার হতে হবে।"
            } else if (newPin != confirmPin) {
              pinError = "উভয় পিন মেলেনি! পুনরায় চেক করুন।"
            } else {
              onSetAppLock(true, newPin)
              showSetPinDialog = false
            }
          },
          modifier = Modifier.testTag("save_pin_button"),
        ) {
          Text("পিন সংরক্ষণ করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSetPinDialog = false }) { Text("বাতিল") }
      },
    )
  }

  // --- App Lock: Verify PIN to Disable Dialog ---
  if (showVerifyPinToDisableDialog) {
    var disablePinInput by remember { mutableStateOf("") }
    var disableError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showVerifyPinToDisableDialog = false },
      title = { Text("অ্যাপ লক বন্ধ করতে পিন দিন") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "নিরাপত্তা নিশ্চিত করতে বর্তমান ৪ সংখ্যার পিন কোডটি প্রবেশ করান।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = disablePinInput,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                disablePinInput = input
                disableError = null
              }
            },
            label = { Text("বর্তমান ৪ সংখ্যার পিন") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("disable_pin_input"),
          )
          if (disableError != null) {
            Text(
              text = disableError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (disablePinInput.length != 4) {
              disableError = "ঠিক ৪ সংখ্যার পিন দিন।"
            } else {
              isChecking = true
              scope.launch {
                val matches = onVerifyPin(disablePinInput)
                isChecking = false
                if (matches) {
                  onSetAppLock(false, null)
                  showVerifyPinToDisableDialog = false
                } else {
                  disableError = "ভুল পিন! সঠিক পিন দিয়ে আবার চেষ্টা করুন।"
                }
              }
            }
          },
          enabled = !isChecking,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_disable_pin_button"),
        ) {
          Text("লক বন্ধ করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showVerifyPinToDisableDialog = false }) { Text("বাতিল") }
      },
    )
  }

  // --- App Lock: Verify Current PIN to Change Dialog ---
  if (showVerifyPinToChangeDialog) {
    var changePinInput by remember { mutableStateOf("") }
    var changeError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showVerifyPinToChangeDialog = false },
      title = { Text("বর্তমান পিন যাচাই করুন") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "নতুন পিন সেট করার পূর্বে বর্তমান ৪ সংখ্যার পিন প্রদান করুন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = changePinInput,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                changePinInput = input
                changeError = null
              }
            },
            label = { Text("বর্তমান পিন") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("change_current_pin_input"),
          )
          if (changeError != null) {
            Text(
              text = changeError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (changePinInput.length != 4) {
              changeError = "ঠিক ৪ সংখ্যার পিন দিন।"
            } else {
              isChecking = true
              scope.launch {
                val matches = onVerifyPin(changePinInput)
                isChecking = false
                if (matches) {
                  showVerifyPinToChangeDialog = false
                  showSetPinDialog = true
                } else {
                  changeError = "বর্তমান পিন ভুল হয়েছে!"
                }
              }
            }
          },
          enabled = !isChecking,
          modifier = Modifier.testTag("confirm_current_pin_button"),
        ) {
          Text("পরবর্তী ধাপ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showVerifyPinToChangeDialog = false }) { Text("বাতিল") }
      },
    )
  }
}
