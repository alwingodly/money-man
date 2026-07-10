package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_millis")
private val NUDGE_DISMISSED_KEY = longPreferencesKey("backup_nudge_dismissed_millis")
private val FIRST_USE_KEY = longPreferencesKey("first_use_millis")

private const val NUDGE_AFTER_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days

/**
 * Decides when to gently remind the user to back up. The clock is measured from the most recent
 * of: the last successful export, the last time they dismissed the reminder, and their first use
 * of the app — so a brand-new user isn't nagged on day one, and dismissing snoozes for another
 * full window. The caller is still expected to only surface the reminder when there's actually
 * data worth protecting.
 */
@Singleton
class BackupReminderRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val shouldNudge: Flow<Boolean> = dataStore.data.map { prefs ->
        val reference = maxOf(
            prefs[LAST_BACKUP_KEY] ?: 0L,
            prefs[NUDGE_DISMISSED_KEY] ?: 0L,
            prefs[FIRST_USE_KEY] ?: 0L,
        )
        // reference == 0 only in the brief window before stampFirstUseIfNeeded() runs; don't nudge
        // then. Once first-use is stamped, the 30-day grace period is measured from that.
        reference != 0L && System.currentTimeMillis() - reference >= NUDGE_AFTER_MILLIS
    }

    suspend fun stampFirstUseIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[FIRST_USE_KEY] == null) prefs[FIRST_USE_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun recordBackup() {
        dataStore.edit { prefs -> prefs[LAST_BACKUP_KEY] = System.currentTimeMillis() }
    }

    suspend fun snoozeNudge() {
        dataStore.edit { prefs -> prefs[NUDGE_DISMISSED_KEY] = System.currentTimeMillis() }
    }
}
