package com.alwin.moneymanager.ui.applock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val repository: AppLockRepository,
) : ViewModel() {

    val isAppLockEnabled: StateFlow<Boolean> = repository.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockOnBackground: StateFlow<Boolean> = repository.lockOnBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAppLockEnabled(enabled) }
    }

    fun setLockOnBackground(enabled: Boolean) {
        viewModelScope.launch { repository.setLockOnBackground(enabled) }
    }
}
