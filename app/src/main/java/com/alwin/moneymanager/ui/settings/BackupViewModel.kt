package com.alwin.moneymanager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupEvent {
    data object ExportSucceeded : BackupEvent
    data object ImportSucceeded : BackupEvent
    data class Failed(val message: String) : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {

    private val _event = MutableStateFlow<BackupEvent?>(null)
    val event: StateFlow<BackupEvent?> = _event.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            _event.value = try {
                repository.exportTo(uri)
                BackupEvent.ExportSucceeded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BackupEvent.Failed(e.message ?: "Export failed")
            }
            _isWorking.value = false
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            _event.value = try {
                repository.importFrom(uri)
                BackupEvent.ImportSucceeded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BackupEvent.Failed(e.message ?: "Import failed — your existing data was not changed")
            }
            _isWorking.value = false
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
