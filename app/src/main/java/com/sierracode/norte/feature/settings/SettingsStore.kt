package com.sierracode.norte.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeConfig { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeConfig: AppThemeConfig = AppThemeConfig.SYSTEM,
    val baseCurrency: String = "MXN",
    val defaultReminderDays: Int = 30
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "norte_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("base_currency")
        val REMINDER_DAYS = intPreferencesKey("reminder_days")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeConfig = runCatching { AppThemeConfig.valueOf(p[Keys.THEME] ?: "") }
                .getOrDefault(AppThemeConfig.SYSTEM),
            baseCurrency = p[Keys.CURRENCY] ?: "MXN",
            defaultReminderDays = p[Keys.REMINDER_DAYS] ?: 30
        )
    }

    suspend fun setThemeConfig(theme: AppThemeConfig) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setBaseCurrency(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = code }
    }

    suspend fun setDefaultReminderDays(days: Int) {
        context.dataStore.edit { it[Keys.REMINDER_DAYS] = days }
    }

    companion object {
        val SUPPORTED_CURRENCIES = listOf("MXN", "USD", "EUR", "CAD")
    }
}