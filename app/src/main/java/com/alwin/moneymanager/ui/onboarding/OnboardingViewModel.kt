package com.alwin.moneymanager.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository,
) : ViewModel() {

    // Defaults to true (already seen) so returning users never see a one-frame flash of the
    // tutorial while the real DataStore value is still loading; a genuinely new user briefly
    // sees the main app for that same one frame instead, before flipping to the tutorial.
    val hasSeenOnboarding: StateFlow<Boolean> = repository.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun markSeen() {
        viewModelScope.launch { repository.setSeen(true) }
    }

    fun resetOnboarding() {
        viewModelScope.launch { repository.setSeen(false) }
    }
}
