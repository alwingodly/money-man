package com.alwin.moneymanager.ui.theme

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.billing.BillingRepository
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
    private val billingRepository: BillingRepository,
) : ViewModel() {

    val themeColor: StateFlow<AppThemeColor> = repository.themeColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeColor.PURPLE)

    val themeStyle: StateFlow<AppThemeStyle> = repository.themeStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeStyle.DEFAULT)

    val isPremium: StateFlow<Boolean> = billingRepository.isPremium

    fun purchasePremium(activity: Activity) = billingRepository.purchasePremium(activity)

    fun setThemeColor(color: AppThemeColor) {
        viewModelScope.launch { repository.setThemeColor(color) }
    }

    fun setThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch { repository.setThemeStyle(style) }
    }
}
