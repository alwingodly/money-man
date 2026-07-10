package com.alwin.moneymanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.BackupReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupReminderViewModel @Inject constructor(
    private val repository: BackupReminderRepository,
) : ViewModel() {

    init {
        // Starts the 30-day grace clock at first Home view for anyone who doesn't have it yet.
        viewModelScope.launch { repository.stampFirstUseIfNeeded() }
    }

    val shouldNudge: StateFlow<Boolean> = repository.shouldNudge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun snooze() {
        viewModelScope.launch { repository.snoozeNudge() }
    }
}
