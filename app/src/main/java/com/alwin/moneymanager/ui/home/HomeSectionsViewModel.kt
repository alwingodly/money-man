package com.alwin.moneymanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.repository.HomeSection
import com.alwin.moneymanager.data.repository.HomeSectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeSectionsViewModel @Inject constructor(
    private val repository: HomeSectionsRepository,
) : ViewModel() {

    val visibility: Map<HomeSection, StateFlow<Boolean>> = HomeSection.entries.associateWith { section ->
        repository.isSectionVisible(section)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    }

    fun setVisible(section: HomeSection, visible: Boolean) {
        viewModelScope.launch { repository.setSectionVisible(section, visible) }
    }
}
