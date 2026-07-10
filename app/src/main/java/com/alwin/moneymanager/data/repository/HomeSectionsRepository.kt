package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class HomeSection(val key: String, val label: String) {
    THIS_MONTH("show_section_this_month", "This Month"),
    PAYMENT_METHOD("show_section_payment_method", "Payment Method"),
    EMI("show_section_emi", "EMI"),
    DEBTS("show_section_debts", "Debts"),
    MONTHLY_AVERAGE("show_section_monthly_average", "Monthly Average"),
    RECENT_ACTIVITY("show_section_recent_activity", "Recent Activity"),
}

@Singleton
class HomeSectionsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun isSectionVisible(section: HomeSection): Flow<Boolean> {
        val key = booleanPreferencesKey(section.key)
        return dataStore.data.map { prefs -> prefs[key] ?: true }
    }

    suspend fun setSectionVisible(section: HomeSection, visible: Boolean) {
        val key = booleanPreferencesKey(section.key)
        dataStore.edit { prefs -> prefs[key] = visible }
    }
}
