package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val LOCK_ON_BACKGROUND_KEY = booleanPreferencesKey("lock_on_background")

@Singleton
class AppLockRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[APP_LOCK_ENABLED_KEY] ?: false }

    val lockOnBackground: Flow<Boolean> = dataStore.data.map { prefs -> prefs[LOCK_ON_BACKGROUND_KEY] ?: true }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[APP_LOCK_ENABLED_KEY] = enabled }
    }

    suspend fun setLockOnBackground(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[LOCK_ON_BACKGROUND_KEY] = enabled }
    }
}
