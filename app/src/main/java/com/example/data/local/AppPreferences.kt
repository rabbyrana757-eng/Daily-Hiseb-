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
}
