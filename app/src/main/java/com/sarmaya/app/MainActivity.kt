package com.sarmaya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sarmaya.app.ui.navigation.SarmayaNavHost
import com.sarmaya.app.ui.theme.SarmayaTheme
import com.sarmaya.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Migrate old SharedPreferences theme to DataStore (one-time)
        migrateThemePreference()

        setContent {
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            SarmayaTheme(
                darkTheme = isDarkTheme ?: isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SarmayaNavHost()
                }
            }
        }
    }

    /**
     * One-time migration from old SharedPreferences ("sarmaya_settings") to DataStore.
     * After migration, the old prefs key is removed so this runs only once.
     */
    private fun migrateThemePreference() {
        val oldPrefs = getSharedPreferences("sarmaya_settings", MODE_PRIVATE)
        if (oldPrefs.contains("dark_theme")) {
            val wasDark = oldPrefs.getBoolean("dark_theme", false)
            val app = application as SarmayaApplication
            kotlinx.coroutines.runBlocking {
                app.container.dataStoreManager.setDarkTheme(
                    if (wasDark) "true" else "false"
                )
            }
            oldPrefs.edit().remove("dark_theme").apply()
        }
    }
}
