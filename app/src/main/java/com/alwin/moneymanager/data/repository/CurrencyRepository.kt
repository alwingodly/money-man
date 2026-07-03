package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alwin.moneymanager.util.CurrencyType
import com.alwin.moneymanager.util.currencyTypeFromName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val CURRENCY_KEY = stringPreferencesKey("currency_type")

@Singleton
class CurrencyRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val currency: Flow<CurrencyType> = dataStore.data.map { prefs -> currencyTypeFromName(prefs[CURRENCY_KEY]) }

    suspend fun setCurrency(type: CurrencyType) {
        dataStore.edit { prefs -> prefs[CURRENCY_KEY] = type.name }
    }
}
