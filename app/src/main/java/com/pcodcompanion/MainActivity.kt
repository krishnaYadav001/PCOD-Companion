package com.pcodcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pcodcompanion.data.repository.SettingsRepository
import com.pcodcompanion.navigation.MainScreen
import com.pcodcompanion.ui.splash.SplashScreen
import com.pcodcompanion.ui.theme.PCODCompanionTheme
import com.pcodcompanion.worker.WorkManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }
        enableEdgeToEdge()

        // Reconcile WorkManager schedule with current settings (e.g. after backup restore).
        // Only on cold-start — config changes (rotation, theme) recreate the activity but
        // the schedule is already correct; re-running it is wasted disk + IPC work.
        if (savedInstanceState == null) {
            WorkManagerHelper.applyReminderSchedule(
                applicationContext,
                enabled = settings.remindersEnabled.value,
                intensity = settings.reminderIntensity.value
            )
        }

        setContent {
            val themePref by settings.themePreference.collectAsState()
            val darkTheme = when (themePref) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            PCODCompanionTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        SplashScreen(onSplashComplete = { showSplash = false })
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark engagement so workers know the user is active and reset the ignore counter
        settings.setLastAppOpenDate(LocalDate.now().toString())
        settings.setReminderIgnoreCount(0)
    }
}
