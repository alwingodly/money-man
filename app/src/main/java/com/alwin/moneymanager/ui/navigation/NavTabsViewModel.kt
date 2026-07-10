package com.alwin.moneymanager.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.NavTabsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavTabsViewModel @Inject constructor(
    private val repository: NavTabsRepository,
) : ViewModel() {

    val showDebts: StateFlow<Boolean> = repository.showDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showSavings: StateFlow<Boolean> = repository.showSavings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setShowDebts(show: Boolean) {
        viewModelScope.launch { repository.setShowDebts(show) }
    }

    fun setShowSavings(show: Boolean) {
        viewModelScope.launch { repository.setShowSavings(show) }
    }
}
