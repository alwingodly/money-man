package com.alwin.moneymanager.ui.applock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

@Composable
fun AppLockGate(
    viewModel: AppLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val lockOnBackground by viewModel.lockOnBackground.collectAsState()

    // Deliberately not rememberSaveable: a process restart should always re-lock.
    var isUnlocked by remember { mutableStateOf(false) }

    DisposableEffect(lockOnBackground) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && lockOnBackground) {
                isUnlocked = false
            }
        }
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (!isAppLockEnabled || isUnlocked) {
        content()
    } else {
        LockScreen(onUnlock = { isUnlocked = true })
    }
}
