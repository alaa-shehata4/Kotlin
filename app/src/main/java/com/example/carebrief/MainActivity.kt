package com.example.carebrief

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.carebrief.core.navigation.Routes
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.theme.CareBriefTheme
import com.example.carebrief.data.SettingsStore
import com.example.carebrief.data.StartScreen
import com.example.carebrief.data.ThemeMode
import com.example.carebrief.data.ai.AiProviders
import com.example.carebrief.presentation.onboarding.OnboardingScreen
import com.example.carebrief.presentation.onboarding.OnboardingStore
import com.example.carebrief.presentation.shell.MainScaffold
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        // Phase 31: leave the splash theme once the app UI takes over.
        setTheme(com.example.carebrief.R.style.Theme_CareBrief)
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val context = LocalContext.current.applicationContext
            val settings = remember { SettingsStore(context) }
            val onboarding = remember { OnboardingStore(context) }
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val startScreen by settings.startScreen.collectAsState(initial = StartScreen.HOME)
            val aiProvider by settings.aiProvider.collectAsState(initial = null)
            val completed by onboarding.completed.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            AiProviders.useRemote =
                aiProvider == com.example.carebrief.data.AiProviderSetting.REMOTE

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CareBriefTheme(darkTheme = darkTheme) {
                Surface(Modifier.fillMaxSize()) {
                    when (completed) {
                        null -> LoadingRow("Loading")
                        false, null -> OnboardingScreen(
                            onFinish = { scope.launch { onboarding.setCompleted(true) } }
                        )
                        else -> androidx.compose.runtime.key(startScreen) {
                            MainScaffold(
                                startDestination = when (startScreen) {
                                    StartScreen.HOME -> Routes.HOME
                                    StartScreen.PEOPLE -> Routes.PEOPLE
                                    StartScreen.NOTES -> Routes.NOTES
                                },
                                onResetOnboarding = { scope.launch { onboarding.setCompleted(false) } }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
