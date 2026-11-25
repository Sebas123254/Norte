package com.sierracode.norte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sierracode.norte.data.settings.AppThemeConfig
import com.sierracode.norte.data.settings.SettingsStore
import com.sierracode.norte.ui.NorteNavHost
import com.sierracode.norte.ui.theme.NorteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = null)
            val isDark = when (settings?.themeConfig) {
                AppThemeConfig.LIGHT -> false
                AppThemeConfig.DARK -> true
                AppThemeConfig.SYSTEM, null -> isSystemInDarkTheme()
            }

            NorteTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NorteNavHost()
                }
            }
        }
    }
}