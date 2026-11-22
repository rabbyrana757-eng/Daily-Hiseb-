package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppPreferences
import com.example.data.local.LocalGuestHisabDataSource
import com.example.data.local.ThemePreference
import com.example.data.model.AccountType
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.repository.HisabRepository
import com.example.ui.HisabViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutFromApp
import com.example.ui.screens.AddEditPartyEntrySheet
import com.example.ui.screens.AddEditPartySheet
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PartyDetailScreen
import com.example.ui.screens.ReportsHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DailyHisabTheme
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicRaised
import com.example.util.ExportBackupManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MainNavTab(val index: Int, val labelBn: String) {
  HOME(0, "হোম"),
  TRANSACTIONS(1, "লেনদেন"),
  REPORTS(3, "রিপোর্ট"),
  SETTINGS(4, "সেটিংস"),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val preferences = remember { AppPreferences(context.applicationContext) }
      val themePref by preferences.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.LIGHT
      )
      val darkTheme =
        when (themePref) {
          ThemePreference.LIGHT -> false
          ThemePreference.DARK -> true
          ThemePreference.SYSTEM -> isSystemInDarkTheme()
        }

      DailyHisabTheme(darkTheme = darkTheme) {
        AppNavigation(
          preferences = preferences,
          themePreference = themePref,
          isDarkTheme = darkTheme,
        )
      }
    }
  }
}

@Composable
fun AppNavigation(
  preferences: AppPreferences,
  themePreference: ThemePreference,
  isDarkTheme: Boolean,
  auth: FirebaseAuth = Firebase.auth,
) {
  val scope = rememberCoroutineScope()
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val guestModeEnabled by preferences.guestModeEnabled.collectAsStateWithLifecycle(initialValue = null)
  val customUserEmail by preferences.customUserEmail.collectAsStateWithLifecycle(initialValue = null)
  val customUserName by preferences.customUserName.collectAsStateWithLifecycle(initialValue = null)
  val appLockEnabled by preferences.appLockEnabled.collectAsStateWithLifecycle(initialValue = false)

  var isAppUnlocked by remember { mutableStateOf(!appLockEnabled) }

  LaunchedEffect(appLockEnabled) {
    if (!appLockEnabled) {
      isAppUnlocked = true
    }
  }

  // Auto-lock when application is sent to background
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, appLockEnabled) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP && appLockEnabled) {
        isAppUnlocked = false
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Intercept with AppLockScreen if enabled and locked
  if (appLockEnabled && !isAppUnlocked) {
    AppLockScreen(
      onVerifyPin = { pin -> preferences.verifyPin(pin) },
      onUnlocked = { isAppUnlocked = true },
    )
    return
  }

  val user = currentUser

  if (user != null) {
    key(user.uid) {
      MainAppShell(
        currentUserId = user.uid,
        userDisplayName = user.displayName,
        userEmail = user.email,
        isGuestMode = false,
        preferences = preferences,
        themePreference = themePreference,
        isDarkTheme = isDarkTheme,
        appLockEnabled = appLockEnabled,
        onSetAppLock = { enabled, pin ->
          scope.launch {
            preferences.setAppLock(enabled, pin)
            if (enabled) isAppUnlocked = true
          }
        },
        onVerifyPin = { pin -> preferences.verifyPin(pin) },
      )
    }
  } else if (!customUserEmail.isNullOrBlank()) {
    key("custom_user_$customUserEmail") {
      MainAppShell(
        currentUserId = "local_user",
        userDisplayName = customUserName ?: customUserEmail,
        userEmail = customUserEmail,
        isGuestMode = true,
        preferences = preferences,
        themePreference = themePreference,
        isDarkTheme = isDarkTheme,
        appLockEnabled = appLockEnabled,
        onSetAppLock = { enabled, pin ->
          scope.launch {
            preferences.setAppLock(enabled, pin)
            if (enabled) isAppUnlocked = true
          }
        },
        onVerifyPin = { pin -> preferences.verifyPin(pin) },
      )
    }
  } else {
    when (guestModeEnabled) {
      null -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        )
      }
      true -> {
        key("guest_local") {
          MainAppShell(
            currentUserId = "guest_local",
            userDisplayName = "অফলাইন গেস্ট",
            userEmail = null,
            isGuestMode = true,
            preferences = preferences,
            themePreference = themePreference,
            isDarkTheme = isDarkTheme,
            appLockEnabled = appLockEnabled,
            onSetAppLock = { enabled, pin ->
              scope.launch {
                preferences.setAppLock(enabled, pin)
                if (enabled) isAppUnlocked = true
              }
            },
            onVerifyPin = { pin -> preferences.verifyPin(pin) },
          )
        }
      }
      false -> {
        AuthScreen(
          onAuthSuccess = {
            scope.launch { preferences.setGuestModeEnabled(false) }
          },
          onContinueAsGuest = {
            scope.launch {
              preferences.clearCustomUserAccount()
              preferences.setGuestModeEnabled(true)
            }
          },
          preferences = preferences,
          onCustomLoginSuccess = { _, _ ->
            // Custom user is registered/logged in via preferences flow
          },
        )
      }
    }
  }
}

@Composable
private fun MainAppShell(
  currentUserId: String,
  userDisplayName: String?,
  userEmail: String?,
  isGuestMode: Boolean,
  preferences: AppPreferences,
  themePreference: ThemePreference,
  isDarkTheme: Boolean,
  appLockEnabled: Boolean = false,
  onSetAppLock: (Boolean, String?) -> Unit = { _, _ -> },
  onVerifyPin: suspend (String) -> Boolean = { true },
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: HisabViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val localGuestDataSource = LocalGuestHisabDataSource(app)
        if (isGuestMode) {
          HisabViewModel(
            repository = localGuestDataSource,
            preferences = preferences,
            currentUserId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isGuestMode = true,
            localGuestDataSource = null,
          )
        } else {
          val databaseId = app.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(databaseId)
          HisabViewModel(
            repository = HisabRepository(db, Firebase.auth),
            preferences = preferences,
            currentUserId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isGuestMode = false,
            localGuestDataSource = localGuestDataSource,
          )
        }
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedNav by remember { mutableStateOf(MainNavTab.HOME) }
  var selectedReportsSubTab by remember { mutableIntStateOf(0) }
  var showAddSheet by remember { mutableStateOf(false) }
  var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
  var isLinkingGoogle by remember { mutableStateOf(false) }

  // Business Tally Khata UI states
  var isBusinessTallyMode by remember { mutableStateOf(true) }
  var selectedPartyId by remember { mutableStateOf<String?>(null) }
  var showAddPartySheet by remember { mutableStateOf(false) }
  var editingParty by remember { mutableStateOf<PartyRecord?>(null) }
  var showAddPartyEntrySheet by remember { mutableStateOf(false) }
  var entryTargetPartyId by remember { mutableStateOf<String?>(null) }
  var initialEntryType by remember { mutableStateOf(PartyEntryType.GIVEN) }
  var editingPartyEntry by remember { mutableStateOf<PartyEntryRecord?>(null) }

  // Show snackbar for errors or feedback messages
  LaunchedEffect(uiState.snackbarMessage, uiState.errorMessage) {
    val msg = uiState.errorMessage ?: uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  // Export & Backup SAF Launchers
  val pdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf"),
    onResult = { uri ->
      if (uri != null) {
        try {
          val reportTxs = uiState.transactions.filter {
            it.accountType == uiState.activeAccount.value &&
              it.year == uiState.selectedReportYear &&
              it.month == uiState.selectedReportMonth
          }
          context.contentResolver.openOutputStream(uri)?.use { out ->
            ExportBackupManager.writeReportPdf(
              context = context,
              outputStream = out,
              accountType = uiState.activeAccount,
              report = uiState.monthlyReport,
              transactions = reportTxs,
            )
          }
          viewModel.showMessage("PDF রিপোর্ট সংরক্ষণ করা হয়েছে")
        } catch (e: Exception) {
          viewModel.showMessage("PDF এক্সপোর্ট ব্যর্থ হয়েছে: ${e.localizedMessage}")
        }
      }
    },
  )

  val excelLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    onResult = { uri ->
      if (uri != null) {
        try {
          val reportTxs = uiState.transactions.filter {
            it.accountType == uiState.activeAccount.value &&
              it.year == uiState.selectedReportYear &&
              it.month == uiState.selectedReportMonth
          }
          context.contentResolver.openOutputStream(uri)?.use { out ->
            ExportBackupManager.writeReportXlsx(
              outputStream = out,
              accountType = uiState.activeAccount,
              report = uiState.monthlyReport,
              transactions = reportTxs,
            )
          }
          viewModel.showMessage("Excel (.xlsx) রিপোর্ট সংরক্ষণ করা হয়েছে")
        } catch (e: Exception) {
          viewModel.showMessage("Excel এক্সপোর্ট ব্যর্থ হয়েছে: ${e.localizedMessage}")
        }
      }
    },
  )

  val backupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
    onResult = { uri ->
      if (uri != null) {
        try {
          val json = ExportBackupManager.serializeBackupJson(
            transactions = uiState.transactions,
            categories = uiState.customCategories,
            budgets = uiState.budgets,
            goals = uiState.savingsGoals,
            parties = uiState.parties,
            partyEntries = uiState.partyEntries,
          )
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
          }
          viewModel.showMessage("সম্পূর্ণ ডেটা ব্যাকআপ সম্পন্ন হয়েছে")
        } catch (e: Exception) {
          viewModel.showMessage("ব্যাকআপ ব্যর্থ হয়েছে: ${e.localizedMessage}")
        }
      }
    },
  )

  val restoreLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) {
        try {
          val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
          }
          if (!jsonText.isNullOrBlank()) {
            val payload = ExportBackupManager.parseBackupJson(jsonText)
            viewModel.restoreBackup(payload)
          }
        } catch (e: Exception) {
          viewModel.showMessage("ব্যাকআপ ফাইল পড়া যায়নি: ${e.localizedMessage}")
        }
      }
    },
  )

  fun triggerPdfExport() {
    val fileName = "MyDailyHisab_${uiState.activeAccount.value}_${uiState.selectedReportMonth}_${uiState.selectedReportYear}.pdf"
    pdfLauncher.launch(fileName)
  }

  fun triggerExcelExport() {
    val fileName = "MyDailyHisab_${uiState.activeAccount.value}_${uiState.selectedReportMonth}_${uiState.selectedReportYear}.xlsx"
    excelLauncher.launch(fileName)
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 840.dp

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (!useNavRail) {
          val neo = HisabTheme.neumorphic
          Box(
            modifier = Modifier
              .padding(top = 6.dp)
              .neumorphicRaised(
                cornerRadius = 24.dp,
                shadowElevation = 7.dp,
              )
              .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
          ) {
            NavigationBar(
              containerColor = Color.Transparent,
              tonalElevation = 0.dp,
              modifier = Modifier.navigationBarsPadding(),
            ) {
              // 1. হোম
              NavigationBarItem(
                selected = selectedNav == MainNavTab.HOME,
                onClick = { selectedNav = MainNavTab.HOME },
                icon = {
                  Icon(
                    if (selectedNav == MainNavTab.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                    contentDescription = "হোম",
                  )
                },
                label = { Text("হোম") },
                modifier = Modifier.testTag("nav_home"),
              )
              // 2. লেনদেন
              NavigationBarItem(
                selected = selectedNav == MainNavTab.TRANSACTIONS,
                onClick = { selectedNav = MainNavTab.TRANSACTIONS },
                icon = {
                  Icon(
                    if (selectedNav == MainNavTab.TRANSACTIONS) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
                    contentDescription = "লেনদেন",
                  )
                },
                label = { Text("লেনদেন") },
                modifier = Modifier.testTag("nav_transactions"),
              )
              // 3. + Add (Center Prominent Neomorphic Orb)
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(64.dp),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .neumorphicCircle(shadowElevation = 5.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(neo.heroGradient))
                    .clickable {
                      if (uiState.activeAccount == AccountType.BUSINESS && isBusinessTallyMode && selectedNav == MainNavTab.HOME) {
                        if (uiState.parties.isEmpty()) {
                          editingParty = null
                          showAddPartySheet = true
                        } else {
                          editingPartyEntry = null
                          entryTargetPartyId = selectedPartyId
                          initialEntryType = PartyEntryType.GIVEN
                          showAddPartyEntrySheet = true
                        }
                      } else {
                        editingTransaction = null
                        showAddSheet = true
                      }
                    }
                    .testTag("nav_add_transaction"),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "লেনদেন যোগ করুন",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              // 4. রিপোর্ট
              NavigationBarItem(
                selected = selectedNav == MainNavTab.REPORTS,
                onClick = { selectedNav = MainNavTab.REPORTS },
                icon = {
                  Icon(
                    if (selectedNav == MainNavTab.REPORTS) Icons.Rounded.Assessment else Icons.Outlined.Assessment,
                    contentDescription = "রিপোর্ট",
                  )
                },
                label = { Text("রিপোর্ট") },
                modifier = Modifier.testTag("nav_reports"),
              )
              // 5. সেটিংস
              NavigationBarItem(
                selected = selectedNav == MainNavTab.SETTINGS,
                onClick = { selectedNav = MainNavTab.SETTINGS },
                icon = {
                  Icon(
                    if (selectedNav == MainNavTab.SETTINGS) Icons.Rounded.Settings else Icons.Outlined.Settings,
                    contentDescription = "সেটিংস",
                  )
                },
                label = { Text("সেটিংস") },
                modifier = Modifier.testTag("nav_settings"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      BackHandler(enabled = selectedNav != MainNavTab.HOME) {
        selectedNav = MainNavTab.HOME
      }
      Row(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(innerPadding)
          .let { mod ->
            if (!useNavRail) mod else mod.navigationBarsPadding()
          },
      ) {
        if (useNavRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            header = {
              Spacer(modifier = Modifier.height(12.dp))
              FloatingActionButton(
                onClick = {
                  if (uiState.activeAccount == AccountType.BUSINESS && isBusinessTallyMode && selectedNav == MainNavTab.HOME) {
                    if (uiState.parties.isEmpty()) {
                      editingParty = null
                      showAddPartySheet = true
                    } else {
                      editingPartyEntry = null
                      entryTargetPartyId = selectedPartyId
                      initialEntryType = PartyEntryType.GIVEN
                      showAddPartyEntrySheet = true
                    }
                  } else {
                    editingTransaction = null
                    showAddSheet = true
                  }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp),
                modifier = Modifier.testTag("rail_add_transaction"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = "লেনদেন যোগ করুন")
              }
            },
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            MainNavTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = selectedNav == tab,
                onClick = { selectedNav = tab },
                icon = {
                  Icon(
                    imageVector = when (tab) {
                      MainNavTab.HOME -> Icons.Rounded.Home
                      MainNavTab.TRANSACTIONS -> Icons.Rounded.ReceiptLong
                      MainNavTab.REPORTS -> Icons.Rounded.Assessment
                      MainNavTab.SETTINGS -> Icons.Rounded.Settings
                    },
                    contentDescription = tab.labelBn,
                  )
                },
                label = { Text(tab.labelBn) },
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 8.dp),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(
            modifier = Modifier
              .widthIn(max = 1080.dp)
              .fillMaxSize(),
          ) {
            when (selectedNav) {
              MainNavTab.HOME -> {
                val activePartySummary =
                  selectedPartyId?.let { pid ->
                    uiState.tallySummary.partySummaries.firstOrNull { it.party.id == pid }
                  }
                if (uiState.activeAccount == AccountType.BUSINESS && activePartySummary != null) {
                  val partyEntriesForDetail =
                    uiState.partyEntries.filter { it.partyId == activePartySummary.party.id }
                  PartyDetailScreen(
                    summary = activePartySummary,
                    entries = partyEntriesForDetail,
                    onBack = { selectedPartyId = null },
                    onEditParty = { party ->
                      editingParty = party
                      showAddPartySheet = true
                    },
                    onDeleteParty = { pid ->
                      viewModel.deleteParty(pid) {
                        selectedPartyId = null
                      }
                    },
                    onAddEntry = { entryType ->
                      editingPartyEntry = null
                      entryTargetPartyId = activePartySummary.party.id
                      initialEntryType = entryType
                      showAddPartyEntrySheet = true
                    },
                    onEditEntry = { entry ->
                      editingPartyEntry = entry
                      entryTargetPartyId = entry.partyId
                      initialEntryType = entry.kind
                      showAddPartyEntrySheet = true
                    },
                    onDeleteEntry = { entryId ->
                      viewModel.deletePartyEntry(entryId)
                    },
                  )
                } else {
                  DashboardScreen(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme,
                    userName = userDisplayName,
                    isBusinessTallyMode = isBusinessTallyMode,
                    onToggleBusinessMode = { isBusinessTallyMode = it },
                    onToggleTheme = {
                      scope.launch {
                        val next = if (isDarkTheme) ThemePreference.LIGHT else ThemePreference.DARK
                        preferences.setThemePreference(next)
                      }
                    },
                    onSwitchAccount = {
                      selectedPartyId = null
                      viewModel.switchAccount(it)
                    },
                    onAddTransaction = {
                      editingTransaction = null
                      showAddSheet = true
                    },
                    onEditTransaction = { tx ->
                      editingTransaction = tx
                      showAddSheet = true
                    },
                    onOpenTransactions = { selectedNav = MainNavTab.TRANSACTIONS },
                    onOpenReportsTab = { subIdx ->
                      selectedReportsSubTab = subIdx
                      selectedNav = MainNavTab.REPORTS
                    },
                    onAddParty = {
                      editingParty = null
                      showAddPartySheet = true
                    },
                    onOpenPartyDetail = { pid -> selectedPartyId = pid },
                    onQuickAddPartyEntry = { party ->
                      editingPartyEntry = null
                      entryTargetPartyId = party.id
                      initialEntryType = PartyEntryType.GIVEN
                      showAddPartyEntrySheet = true
                    },
                  )
                }
              }

              MainNavTab.TRANSACTIONS -> TransactionsScreen(
                uiState = uiState,
                onSwitchAccount = { viewModel.switchAccount(it) },
                onEditTransaction = { tx ->
                  editingTransaction = tx
                  showAddSheet = true
                },
                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                onEditPartyEntry = { entry ->
                  editingPartyEntry = entry
                  entryTargetPartyId = entry.partyId
                  initialEntryType = entry.kind
                  showAddPartyEntrySheet = true
                },
                onDeletePartyEntry = { viewModel.deletePartyEntry(it) },
              )

              MainNavTab.REPORTS -> ReportsHubScreen(
                uiState = uiState,
                selectedSubTab = selectedReportsSubTab,
                onSelectSubTab = { selectedReportsSubTab = it },
                onSwitchAccount = { viewModel.switchAccount(it) },
                onSelectMonthYear = { y, m -> viewModel.selectReportMonthYear(y, m) },
                onExportPdf = { triggerPdfExport() },
                onExportExcel = { triggerExcelExport() },
                onSaveBudget = { existingId, cat, limit, year, month ->
                  viewModel.saveBudget(
                    existingId = existingId,
                    accountType = uiState.activeAccount,
                    category = cat,
                    limitAmount = limit,
                    year = year,
                    month = month,
                    onSuccess = {},
                  )
                },
                onDeleteBudget = { viewModel.deleteBudget(it) },
                onSaveSavingsGoal = { editingId, name, target, saved, dateMillis ->
                  viewModel.saveSavingsGoal(
                    editingId = editingId,
                    accountType = uiState.activeAccount,
                    name = name,
                    targetAmount = target,
                    savedAmount = saved,
                    targetDateMillis = dateMillis,
                    onSuccess = {},
                  )
                },
                onAddSavingsContribution = { goal, add ->
                  viewModel.addSavingsContribution(goal, add)
                },
                onDeleteSavingsGoal = { viewModel.deleteSavingsGoal(it) },
              )

              MainNavTab.SETTINGS -> SettingsScreen(
                uiState = uiState,
                userName = userDisplayName,
                userEmail = userEmail,
                themePreference = themePreference,
                onChangeTheme = { pref ->
                  scope.launch { preferences.setThemePreference(pref) }
                },
                onSwitchAccount = { viewModel.switchAccount(it) },
                onAddCustomCategory = { name, type ->
                  viewModel.addCustomCategory(name, type)
                },
                onDeleteCustomCategory = { viewModel.deleteCustomCategory(it) },
                onExportPdf = { triggerPdfExport() },
                onExportExcel = { triggerExcelExport() },
                onBackupData = {
                  backupLauncher.launch("MyDailyHisab_Backup_${System.currentTimeMillis()}.json")
                },
                onRestoreData = {
                  restoreLauncher.launch(arrayOf("application/json", "*/*"))
                },
                onSignOut = {
                  scope.launch {
                    preferences.clearCustomUserAccount()
                    if (isGuestMode) {
                      preferences.setGuestModeEnabled(false)
                    } else {
                      signOutFromApp(credentialManager = credentialManager, scope = scope)
                    }
                  }
                },
                isGuestMode = isGuestMode,
                isLinkingGoogle = isLinkingGoogle,
                appLockEnabled = appLockEnabled,
                onSetAppLock = onSetAppLock,
                onVerifyPin = onVerifyPin,
                onLinkGoogleAccount = {
                  isLinkingGoogle = true
                  scope.launch {
                    preferences.setMigrateGuestOnSignIn(true)
                    onGoogleSignInClicked(
                      context = context,
                      credentialManager = credentialManager,
                      onAuthSuccess = {
                        isLinkingGoogle = false
                        scope.launch {
                          preferences.clearCustomUserAccount()
                          preferences.setGuestModeEnabled(false)
                        }
                      },
                      onAuthError = { msg ->
                        isLinkingGoogle = false
                        scope.launch { preferences.setMigrateGuestOnSignIn(false) }
                        viewModel.showMessage(msg)
                      },
                      scope = scope,
                      onAuthCancelled = {
                        isLinkingGoogle = false
                        scope.launch { preferences.setMigrateGuestOnSignIn(false) }
                      },
                    )
                  }
                },
              )
            }
          }
        }
      }
    }
  }

  if (showAddSheet) {
    AddEditTransactionSheet(
      editingTransaction = editingTransaction,
      defaultAccount = uiState.activeAccount,
      customCategories = uiState.customCategories,
      isSubmitting = uiState.isSubmitting,
      onSave = { editingId, account, type, amount, category, pm, dateMillis, note, receipt, reqId ->
        viewModel.saveTransaction(
          editingId = editingId,
          accountType = account,
          type = type,
          amount = amount,
          category = category,
          paymentMethod = pm,
          dateMillis = dateMillis,
          note = note,
          receiptBase64 = receipt,
          clientRequestId = reqId,
          onSuccess = {
            showAddSheet = false
            editingTransaction = null
          },
        )
      },
      onAddCustomCategory = { name, type ->
        viewModel.addCustomCategory(name, type)
      },
      onDismiss = {
        showAddSheet = false
        editingTransaction = null
      },
    )
  }

  if (showAddPartySheet) {
    AddEditPartySheet(
      editingParty = editingParty,
      defaultPartyType = PartyType.CUSTOMER,
      isSubmitting = uiState.isSubmitting,
      onSave = { editingId, partyType, name, phone, address, note ->
        viewModel.saveParty(
          editingId = editingId,
          partyType = partyType,
          name = name,
          phone = phone,
          address = address,
          note = note,
          onSuccess = { createdOrUpdatedId ->
            showAddPartySheet = false
            editingParty = null
            if (showAddPartyEntrySheet) {
              entryTargetPartyId = createdOrUpdatedId
            }
          },
        )
      },
      onDismiss = {
        showAddPartySheet = false
        editingParty = null
      },
    )
  }

  if (showAddPartyEntrySheet) {
    AddEditPartyEntrySheet(
      parties = uiState.parties,
      initialPartyId = entryTargetPartyId,
      initialEntryType = initialEntryType,
      editingEntry = editingPartyEntry,
      isSubmitting = uiState.isSubmitting,
      onQuickAddParty = {
        editingParty = null
        showAddPartySheet = true
      },
      onSave = { editingId, partyId, entryType, amount, pm, dateMillis, note, reqId ->
        viewModel.savePartyEntry(
          editingId = editingId,
          partyId = partyId,
          entryType = entryType,
          amount = amount,
          paymentMethod = pm,
          dateMillis = dateMillis,
          note = note,
          clientRequestId = reqId,
          onSuccess = {
            showAddPartyEntrySheet = false
            editingPartyEntry = null
          },
        )
      },
      onDismiss = {
        showAddPartyEntrySheet = false
        editingPartyEntry = null
      },
    )
  }
}
