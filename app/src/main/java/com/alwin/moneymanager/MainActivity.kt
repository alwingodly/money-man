package com.alwin.moneymanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.ui.applock.AppLockGate
import com.alwin.moneymanager.ui.navigation.MoneyManagerNavHost
import com.alwin.moneymanager.ui.onboarding.OnboardingScreen
import com.alwin.moneymanager.ui.onboarding.OnboardingViewModel
import com.alwin.moneymanager.ui.settings.CurrencyViewModel
import com.alwin.moneymanager.ui.theme.MoneyManagerTheme
import com.alwin.moneymanager.ui.theme.ThemeViewModel
import com.alwin.moneymanager.util.currentCurrency
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Financial data on screen: block screenshots/screen recording and hide the app's
        // content in the recent-apps switcher thumbnail.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeColor by themeViewModel.themeColor.collectAsState()
            val themeStyle by themeViewModel.themeStyle.collectAsState()
            val currencyViewModel: CurrencyViewModel = hiltViewModel()
            currentCurrency = currencyViewModel.currency.collectAsState().value
            MoneyManagerTheme(seedColor = themeColor.seed, themeStyle = themeStyle) {
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val hasSeenOnboarding by onboardingViewModel.hasSeenOnboarding.collectAsState()
                if (hasSeenOnboarding) {
                    AppLockGate {
                        MoneyManagerNavHost()
                    }
                } else {
                    OnboardingScreen(onFinish = onboardingViewModel::markSeen)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
