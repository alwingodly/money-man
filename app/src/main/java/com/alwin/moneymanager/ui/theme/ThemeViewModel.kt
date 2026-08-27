package com.alwin.moneymanager.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository,
) : ViewModel() {

    val themeColor: StateFlow<AppThemeColor> = repository.themeColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeColor.NAVY)

    val themeStyle: StateFlow<AppThemeStyle> = repository.themeStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeStyle.DEFAULT)

    val themeMode: StateFlow<AppThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.LIGHT)

    fun setThemeColor(color: AppThemeColor) {
        viewModelScope.launch { repository.setThemeColor(color) }
    }

    fun setThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch { repository.setThemeStyle(style) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}
