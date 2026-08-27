package com.alwin.moneymanager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alwin.moneymanager.data.backup.AutoBackupRepository
import com.alwin.moneymanager.data.backup.BackupRepository
import com.alwin.moneymanager.data.repository.BackupReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupEvent {
    data object ExportSucceeded : BackupEvent
    data object ImportSucceeded : BackupEvent
    data object AutoBackupEnabled : BackupEvent
    data class Failed(val message: String) : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val autoBackupRepository: AutoBackupRepository,
    backupReminderRepository: BackupReminderRepository,
) : ViewModel() {

    private val _event = MutableStateFlow<BackupEvent?>(null)
    val event: StateFlow<BackupEvent?> = _event.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    val isAutoBackupEnabled: StateFlow<Boolean> = autoBackupRepository.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastBackupMillis: StateFlow<Long?> = backupReminderRepository.lastBackupMillis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun enableAutoBackup(treeUri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            _event.value = try {
                val fileUri = autoBackupRepository.enable(treeUri)
                repository.exportTo(fileUri)
                BackupEvent.AutoBackupEnabled
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                autoBackupRepository.disable()
                BackupEvent.Failed(e.message ?: "Could not enable auto backup")
            }
            _isWorking.value = false
        }
    }

    fun disableAutoBackup() {
        viewModelScope.launch { autoBackupRepository.disable() }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
