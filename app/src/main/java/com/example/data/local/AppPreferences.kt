package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemePreference(val value: String, val labelBn: String) {
  LIGHT("light", "লাইট মোড"),
  DARK("dark", "ডার্ক মোড"),
  SYSTEM("system", "সিস্টেম ডিফল্ট");

  companion object {
    fun fromValue(value: String?): ThemePreference =
      entries.firstOrNull { it.value == value } ?: LIGHT
  }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_hisab_prefs")

class AppPreferences(private val context: Context) {
  private val themeKey = stringPreferencesKey("theme_mode")
  private val accountKey = stringPreferencesKey("active_account")
  private val guestModeKey = booleanPreferencesKey("guest_mode_enabled")
  private val migrateGuestKey = booleanPreferencesKey("migrate_guest_on_sign_in")

  // App Lock preferences
  private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
  private val appLockPinKey = stringPreferencesKey("app_lock_pin_hash")

  // Custom Gmail & Password user credentials
  private val customEmailKey = stringPreferencesKey("custom_user_email")
  private val customNameKey = stringPreferencesKey("custom_user_name")
  private val customPasswordHashKey = stringPreferencesKey("custom_user_password_hash")

  val themePreference: Flow<ThemePreference> =
    context.dataStore.data.map { prefs ->
      ThemePreference.fromValue(prefs[themeKey])
    }

  val activeAccount: Flow<AccountType> =
    context.dataStore.data.map { prefs ->
      AccountType.fromValue(prefs[accountKey])
    }

  val guestModeEnabled: Flow<Boolean> =
    context.dataStore.data.map { prefs ->
      prefs[guestModeKey] ?: false
    }

  val appLockEnabled: Flow<Boolean> =
    context.dataStore.data.map { prefs ->
      prefs[appLockEnabledKey] ?: false
    }

  val appLockPinHash: Flow<String?> =
    context.dataStore.data.map { prefs ->
      prefs[appLockPinKey]
    }

  val customUserEmail: Flow<String?> =
    context.dataStore.data.map { prefs ->
      prefs[customEmailKey]
    }

  val customUserName: Flow<String?> =
    context.dataStore.data.map { prefs ->
      prefs[customNameKey]
    }

  suspend fun setThemePreference(pref: ThemePreference) {
    context.dataStore.edit { prefs ->
      prefs[themeKey] = pref.value
    }
  }

  suspend fun setActiveAccount(account: AccountType) {
    context.dataStore.edit { prefs ->
      prefs[accountKey] = account.value
    }
  }

  suspend fun setGuestModeEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[guestModeKey] = enabled
    }
  }

  suspend fun setMigrateGuestOnSignIn(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[migrateGuestKey] = enabled
    }
  }

  suspend fun consumeMigrateGuestOnSignIn(): Boolean {
    var shouldMigrate = false
    context.dataStore.edit { prefs ->
      shouldMigrate = prefs[migrateGuestKey] ?: false
      if (shouldMigrate) {
        prefs[migrateGuestKey] = false
      }
    }
    return shouldMigrate
  }

  // --- App Lock (4-digit PIN) ---

  suspend fun setAppLock(enabled: Boolean, rawPin: String? = null) {
    context.dataStore.edit { prefs ->
      prefs[appLockEnabledKey] = enabled
      if (rawPin != null) {
        prefs[appLockPinKey] = hashSha256(rawPin)
      } else if (!enabled) {
        prefs.remove(appLockPinKey)
      }
    }
  }

  suspend fun verifyPin(enteredPin: String): Boolean {
    val currentHash = context.dataStore.data.map { it[appLockPinKey] }.first() ?: return false
    return hashSha256(enteredPin) == currentHash
  }

  // --- Custom Gmail & Password Account ---

  suspend fun saveCustomUserAccount(email: String, name: String, password: String) {
    context.dataStore.edit { prefs ->
      prefs[customEmailKey] = email.trim().lowercase()
      prefs[customNameKey] = name.trim()
      prefs[customPasswordHashKey] = hashSha256(password)
      prefs[guestModeKey] = true
    }
  }

  suspend fun verifyCustomUser(email: String, password: String): Boolean {
    val prefs = context.dataStore.data.first()
    val savedEmail = prefs[customEmailKey]
    val savedHash = prefs[customPasswordHashKey]
    if (savedEmail.isNullOrBlank() || savedHash.isNullOrBlank()) return false
    return savedEmail.equals(email.trim(), ignoreCase = true) && hashSha256(password) == savedHash
  }

  suspend fun clearCustomUserAccount() {
    context.dataStore.edit { prefs ->
      prefs.remove(customEmailKey)
      prefs.remove(customNameKey)
      prefs.remove(customPasswordHashKey)
    }
  }

  companion object {
    fun hashSha256(input: String): String {
      val md = java.security.MessageDigest.getInstance("SHA-256")
      val digest = md.digest(input.toByteArray(Charsets.UTF_8))
      return digest.joinToString("") { "%02x".format(it) }
    }
  }
}
