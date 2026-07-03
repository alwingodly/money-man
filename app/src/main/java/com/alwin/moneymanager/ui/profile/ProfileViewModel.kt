package com.alwin.moneymanager.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    val name: StateFlow<String?> = repository.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val photoPath: StateFlow<String?> = repository.photoPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setName(name: String) {
        viewModelScope.launch { repository.setName(name) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch { repository.setPhoto(uri) }
    }
}
