package com.alwin.moneymanager.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.HomeRepository
import com.alwin.moneymanager.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoadedName(val loaded: Boolean, val name: String)

data class ProfilePulse(
    val today: Double,
    val month: Double,
    val extra: Double,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    homeRepository: HomeRepository,
) : ViewModel() {

    val name: StateFlow<String?> = repository.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Becomes ready only after DataStore has emitted, so a missing name is not confused with loading. */
    val loadedName: StateFlow<LoadedName> = repository.name
        .map { LoadedName(loaded = true, name = it?.trim().orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadedName(loaded = false, name = ""))

    val photoPath: StateFlow<String?> = repository.photoPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pulse: StateFlow<ProfilePulse?> = homeRepository.getHomeSummary()
        .map { ProfilePulse(today = it.todayTotal, month = it.monthExpenseOnly, extra = it.extraThisMonth) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setName(name: String) {
        viewModelScope.launch { repository.setName(name) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch { repository.setPhoto(uri) }
    }

    fun clearPhoto() {
        viewModelScope.launch { repository.clearPhoto() }
    }
}
