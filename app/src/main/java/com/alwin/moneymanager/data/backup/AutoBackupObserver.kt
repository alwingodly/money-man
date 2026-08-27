package com.alwin.moneymanager.data.backup

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the rolling JSON snapshot when the app goes to the background, so a later uninstall
 * still leaves a file in the folder the user picked. Brief background hops (task switch, a
 * notification shade peek) are ignored; a full export also waits at least five minutes.
 */
@Singleton
class AutoBackupObserver @Inject constructor(
    private val backupRepository: BackupRepository,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var pendingJob: Job? = null

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        pendingJob?.cancel()
        pendingJob = null
    }

    override fun onStop(owner: LifecycleOwner) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(1_500)
            mutex.withLock { backupRepository.tryAutoExport() }
        }
    }
}
