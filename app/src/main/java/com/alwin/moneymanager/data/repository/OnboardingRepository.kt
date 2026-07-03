package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

@Singleton
class OnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] ?: false }

    suspend fun setSeen(seen: Boolean) {
        dataStore.edit { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] = seen }
    }
}
