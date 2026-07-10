package com.alwin.moneymanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alwin.moneymanager.ui.theme.AppThemeColor
import com.alwin.moneymanager.ui.theme.AppThemeStyle
import com.alwin.moneymanager.ui.theme.appThemeColorFromName
import com.alwin.moneymanager.ui.theme.appThemeStyleFromName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
private val THEME_STYLE_KEY = stringPreferencesKey("theme_style")

@Singleton
class ThemeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val themeColor: Flow<AppThemeColor> = dataStore.data.map { prefs ->
        appThemeColorFromName(prefs[THEME_COLOR_KEY])
    }

    suspend fun setThemeColor(color: AppThemeColor) {
        dataStore.edit { prefs -> prefs[THEME_COLOR_KEY] = color.name }
    }

    val themeStyle: Flow<AppThemeStyle> = dataStore.data.map { prefs ->
        appThemeStyleFromName(prefs[THEME_STYLE_KEY])
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        dataStore.edit { prefs -> prefs[THEME_STYLE_KEY] = style.name }
    }
}
