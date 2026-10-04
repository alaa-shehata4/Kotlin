package com.example.carebrief

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.carebrief.core.ui.components.LoadingRow
import com.example.carebrief.core.ui.theme.CareBriefTheme
import com.example.carebrief.presentation.onboarding.OnboardingScreen
import com.example.carebrief.presentation.onboarding.OnboardingStore
import com.example.carebrief.presentation.shell.MainScaffold
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CareBriefTheme {
                Surface(Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val store = remember { OnboardingStore(context.applicationContext) }
                    val completed by store.completed.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()

                    when (completed) {
                        null -> LoadingRow("Loading")
                        false, null -> OnboardingScreen(
                            onFinish = { scope.launch { store.setCompleted(true) } }
                        )
                        else -> MainScaffold(
                            onResetOnboarding = { scope.launch { store.setCompleted(false) } }
                        )
                    }
                }
            }
        }
    }
}
