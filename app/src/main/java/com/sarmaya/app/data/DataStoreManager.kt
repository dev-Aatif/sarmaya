package com.sarmaya.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sarmaya_preferences")

/**
 * Centralized preferences manager using Jetpack DataStore.
 * Replaces SharedPreferences for all app settings.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val KEY_DARK_THEME = stringPreferencesKey("dark_theme") // "true", "false", or "system"
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val KEY_DISMISSED_UPDATE_TAG = stringPreferencesKey("dismissed_update_tag")
        val KEY_NOTIFICATIONS_PORTFOLIO = booleanPreferencesKey("notif_portfolio")
        val KEY_NOTIFICATIONS_MARKET = booleanPreferencesKey("notif_market")
        val KEY_NOTIFICATIONS_UPDATES = booleanPreferencesKey("notif_updates")
    }

    // ─── Username ───

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = name
        }
    }

    // ─── Onboarding ───

    val hasOnboarded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_ONBOARDED] ?: false
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_ONBOARDED] = value
        }
    }

    // ─── Theme ───
    // Returns: "true" (dark), "false" (light), "system" (follow system)

    val darkThemePreference: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: "system"
    }

    suspend fun setDarkTheme(preference: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = preference
        }
    }

    // ─── Auto Refresh ───

    val autoRefreshEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REFRESH] ?: false
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH] = enabled
        }
    }

    // ─── Dismissed Update ───

    val dismissedUpdateTag: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISMISSED_UPDATE_TAG] ?: ""
    }

    suspend fun setDismissedUpdateTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISMISSED_UPDATE_TAG] = tag
        }
    }

    // ─── Notification Preferences ───

    val notificationsPortfolio: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_PORTFOLIO] ?: true
    }

    val notificationsMarket: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_MARKET] ?: true
    }

    val notificationsUpdates: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_UPDATES] ?: true
    }

    suspend fun setNotificationPreference(key: Preferences.Key<Boolean>, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }
}
