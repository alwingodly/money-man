package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val SHOW_DEBTS_TAB = booleanPreferencesKey("show_debts_tab")
private val SHOW_SAVINGS_TAB = booleanPreferencesKey("show_savings_tab")

/**
 * Which optional bottom-nav tabs are shown. Home/EMIs/Expenses are core and always present; Debts
 * and Savings can be hidden by users who don't use them (also keeps the bar under Material's 5-tab
 * ceiling). Both default to on.
 */
@Singleton
class NavTabsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val showDebts: Flow<Boolean> = dataStore.data.map { it[SHOW_DEBTS_TAB] ?: true }
    val showSavings: Flow<Boolean> = dataStore.data.map { it[SHOW_SAVINGS_TAB] ?: true }

    suspend fun setShowDebts(show: Boolean) = dataStore.edit { it[SHOW_DEBTS_TAB] = show }
    suspend fun setShowSavings(show: Boolean) = dataStore.edit { it[SHOW_SAVINGS_TAB] = show }
}
