package com.alwin.moneymanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.CurrencyRepository
import com.alwin.moneymanager.util.CurrencyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository,
) : ViewModel() {

    val currency: StateFlow<CurrencyType> = repository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyType.RUPEE)

    fun setCurrency(type: CurrencyType) {
        viewModelScope.launch { repository.setCurrency(type) }
    }
}
