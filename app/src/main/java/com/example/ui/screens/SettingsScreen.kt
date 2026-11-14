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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
  modifier: Modifier = Modifier,
) {
  var showAddCatDialog by remember { mutableStateOf(false) }
  var pendingDeleteCat by remember { mutableStateOf<CategoryRecord?>(null) }
  var showSignOutConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "সেটিংস ও ব্যাকআপ",
        style = MaterialTheme.typography.headlineSmall,
      )
    }

    // User Account & Cloud Sync Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = (userName?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = userName?.takeIf { it.isNotBlank() } ?: "ব্যবহারকারী",
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
                Icons.Rounded.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Firebase ক্লাউড সিঙ্ক সক্রিয়",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    // Account Switcher
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

    // Sign Out
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
        Text("সাইন আউট করুন", fontWeight = FontWeight.Bold)
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
      title = { Text("সাইন আউট করবেন?") },
      text = { Text("আপনার সব হিসাব Firebase ক্লাউডে নিরাপদে সংরক্ষিত আছে। পুনরায় লগইন করলেই সব তথ্য ফিরে পাবেন।") },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirm = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("সাইন আউট") }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirm = false }) { Text("বাতিল") }
      },
    )
  }
}
