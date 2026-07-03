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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeColor.PURPLE)

    fun setThemeColor(color: AppThemeColor) {
        viewModelScope.launch { repository.setThemeColor(color) }
    }
}
