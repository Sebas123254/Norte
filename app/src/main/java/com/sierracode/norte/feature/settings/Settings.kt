package com.sierracode.norte.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.CurrencyConverter
import com.sierracode.norte.data.settings.AppSettings
import com.sierracode.norte.data.settings.AppThemeConfig
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val currencyConverter: CurrencyConverter
) : ViewModel() {

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    var isConverting by mutableStateOf(false)
        private set

    fun setTheme(theme: AppThemeConfig) = viewModelScope.launch {
        settingsStore.setThemeConfig(theme)
    }

    fun setReminderDays(days: Int) = viewModelScope.launch {
        settingsStore.setDefaultReminderDays(days)
    }

    fun convertCurrency(newCurrency: String, rate: Double, onFinished: () -> Unit) = viewModelScope.launch {
        isConverting = true
        try {
            currencyConverter.convertBaseCurrency(newCurrency, rate)
            onFinished()
        } finally {
            isConverting = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    nav: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val settings by vm.settings.collectAsState()
    var targetCurrencyForChange by remember { mutableStateOf<String?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sección de Tema
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BrightnessMedium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeConfig.entries.forEach { config ->
                            val label = when (config) {
                                AppThemeConfig.SYSTEM -> stringResource(R.string.settings_theme_system)
                                AppThemeConfig.LIGHT -> stringResource(R.string.settings_theme_light)
                                AppThemeConfig.DARK -> stringResource(R.string.settings_theme_dark)
                            }
                            FilterChip(
                                selected = settings.themeConfig == config,
                                onClick = { vm.setTheme(config) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Sección de Moneda Base
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_currency_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.settings_currency_current, settings.baseCurrency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsStore.SUPPORTED_CURRENCIES.forEach { currency ->
                            val isCurrent = settings.baseCurrency == currency
                            ElevatedFilterChip(
                                selected = isCurrent,
                                onClick = {
                                    if (!isCurrent) {
                                        targetCurrencyForChange = currency
                                    }
                                },
                                label = { Text(currency) }
                            )
                        }
                    }
                }
            }

            // Sección de Anticipación de Avisos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showReminderDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_reminders_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_reminders_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_reminders_days_format, settings.defaultReminderDays),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Diálogo de Días de Anticipación
    if (showReminderDialog) {
        var daysText by remember { mutableStateOf(settings.defaultReminderDays.toString()) }
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text(stringResource(R.string.settings_reminders_days_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.settings_reminders_days_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val days = daysText.toIntOrNull() ?: 30
                        if (days > 0) {
                            vm.setReminderDays(days)
                            showReminderDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Diálogo de Cambio de Moneda Base con advertencia y tasa
    targetCurrencyForChange?.let { newCurrency ->
        ChangeCurrencyDialog(
            currentCurrency = settings.baseCurrency,
            newCurrency = newCurrency,
            isConverting = vm.isConverting,
            onDismiss = { targetCurrencyForChange = null },
            onConfirm = { rate ->
                vm.convertCurrency(newCurrency, rate) {
                    targetCurrencyForChange = null
                }
            }
        )
    }
}

@Composable
private fun ChangeCurrencyDialog(
    currentCurrency: String,
    newCurrency: String,
    isConverting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rateText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidRateMsg = stringResource(R.string.settings_currency_error_invalid_rate)

    AlertDialog(
        onDismissRequest = { if (!isConverting) onDismiss() },
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text(stringResource(R.string.settings_currency_change_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_currency_change_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = stringResource(R.string.settings_currency_rate_label, currentCurrency, newCurrency),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        errorMessage = null
                    },
                    placeholder = { Text(stringResource(R.string.settings_currency_rate_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                )

                if (isConverting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_currency_converting))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isConverting,
                onClick = {
                    val rate = rateText.replace(',', '.').toDoubleOrNull()
                    if (rate == null || rate <= 0.0) {
                        errorMessage = invalidRateMsg
                    } else {
                        onConfirm(rate)
                    }
                }
            ) {
                Text(stringResource(R.string.settings_currency_confirm_btn), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isConverting,
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
